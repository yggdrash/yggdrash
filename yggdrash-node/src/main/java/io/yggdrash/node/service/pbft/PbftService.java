package io.yggdrash.node.service.pbft;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockBody;
import io.yggdrash.core.blockchain.BlockHeader;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.blockchain.pbft.PbftBlock;
import io.yggdrash.core.blockchain.pbft.PbftBlockChain;
import io.yggdrash.core.blockchain.pbft.PbftMessage;
import io.yggdrash.core.blockchain.pbft.PbftMessage.Type;
import io.yggdrash.core.blockchain.pbft.PbftMessageSet;
import io.yggdrash.core.blockchain.pbft.PbftStatus;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.core.wallet.Wallet;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

import static io.yggdrash.common.util.Utils.sleep;
import static io.yggdrash.core.blockchain.pbft.PbftMessage.Type.PREPAREM;
import static io.yggdrash.core.blockchain.pbft.PbftMessage.Type.PREPREPA;
import static io.yggdrash.core.blockchain.pbft.PbftMessage.Type.VIEWCHAN;

@Profile("validator")
@Service
public class PbftService implements CommandLineRunner {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(PbftService.class);
    private static final int FAIL_COUNT = 2;

    private final boolean isValidator;
    private final int consensusCount;

    private final Wallet wallet;
    private final PbftBlockChain blockChain;

    private final PbftClientStub myNode;
    private final Map<String, PbftClientStub> totalValidatorMap = new TreeMap<>();

    private final ReentrantLock lock = new ReentrantLock();

    private boolean isActive;
    private boolean isSynced;
    private boolean isPrePrepared;
    private boolean isPrepared;
    private boolean isCommitted;
    private boolean isViewChanged;

    private boolean isPrimary;

    private int failCount;
    private final Map<String, PbftMessage> viewChangeMap = new TreeMap<>();

    @Autowired
    public PbftService(Wallet wallet, PbftBlockChain pbftBlockChain) {
        this.wallet = wallet;
        this.blockChain = pbftBlockChain;

        this.myNode = initMyNode();
        initTotalValidator();

        this.isValidator = isValidator();
        if (totalValidatorMap.isEmpty()) {
            throw new NotValidateException();
        } else {
            int bftCount = (totalValidatorMap.size() - 1) / 3;
            this.consensusCount = bftCount * 2 + 1;
        }

        this.isActive = false;
        this.isSynced = false;
        this.isPrePrepared = false;
        this.isPrepared = false;
        this.isCommitted = false;
        this.isViewChanged = false;
        this.failCount = 0;
    }

    @Override
    public void run(String... args) {
        printInitInfo();
    }

    // todo: chage cron setting to config file or genesis ...
    @Scheduled(cron = "*/2 * * * * *")
    public void mainScheduler() {

        checkNode();

        checkPrimary();

        // make PrePrepare msg
        lock.lock();
        PbftMessage prePrepareMsg = makePrePrepareMsg();
        lock.unlock();
        if (prePrepareMsg != null) {
            multicastMessage(prePrepareMsg);
        }

        sleep(500);

        // make Prepare msg
        lock.lock();
        PbftMessage prepareMsg = makePrepareMsg();
        lock.unlock();
        if (prepareMsg != null) {
            multicastMessage(prepareMsg);
        }

        sleep(500);

        // make commit msg
        lock.lock();
        PbftMessage commitMsg = makeCommitMsg();
        lock.unlock();
        if (commitMsg != null) {
            multicastMessage(commitMsg);
        }

        sleep(500);

        lock.lock();
        confirmFinalBlock();
        lock.unlock();

        lock.lock();
        PbftMessage viewChangeMsg = makeViewChangeMsg();
        lock.unlock();
        if (viewChangeMsg != null) {
            multicastMessage(viewChangeMsg);
        }

        loggingStatus();

    }

    private void loggingStatus() {
        log.debug("failCount: {}", failCount);
        log.debug("isAcitve: {}", isActive);
        log.debug("isSynced: {}", isSynced);
        log.debug("isPrePrepared: {}", isPrePrepared);
        log.debug("isPrepared: {}", isPrepared);
        log.debug("isCommitted: {}", isCommitted);
        log.debug("isViewChanged: {}", isViewChanged);
        log.debug("unConfirmedMsgMap size: {}", blockChain.getUnConfirmedMsgMap().size());
        log.debug("TxStore unConfirmed Tx.size: {}", blockChain.getTransactionStore().getUnconfirmedTxs().size());

        PbftBlock lastBlock = this.blockChain.getLastConfirmedBlock();
        if (lastBlock.getPbftMessageSet() == null) {
            return;
        }

        log.info("PbftBlock viewNumber={}, ViewChangeMapSize={},",
                lastBlock.getPbftMessageSet().getPrePrepare().getViewNumber(),
                lastBlock.getPbftMessageSet().getViewChangeMap().size());
        log.debug("prepareMapSize={}, commitMapSize={}",
                lastBlock.getPbftMessageSet().getPrepareMap().size(),
                lastBlock.getPbftMessageSet().getCommitMap().size());
        log.debug("lastBlock index={}, hash={}, addrHex={}", lastBlock.getIndex(), lastBlock.getHashHex(),
                lastBlock.getBlock().getAddressHex());
    }

    private void multicastMessage(PbftMessage message) {
        for (PbftClientStub client : totalValidatorMap.values()) {
            if (client.isMyClient()) {
                continue;
            }
            if (client.isRunning()) {
                try {
                    client.multicastPbftMessage(PbftMessage.toProto(message));
                } catch (Exception e) {
                    log.debug("multicast exception: {}", e.getMessage());
                    log.debug("client: {}", client.getId());
                    log.debug("message: {}", message);
                    // continue
                }
            }
        }
    }

    private PbftMessage makePrePrepareMsg() {
        if (!this.isValidator
                || !this.isActive
                || !this.isSynced
                || !this.isPrimary
                || this.isPrePrepared) {
            return null;
        }

        long seqNumber = this.blockChain.getLastConfirmedBlock().getIndex() + 1;
        byte[] prevBlockHash = this.blockChain.getLastConfirmedBlock().getHash();

        Block newBlock = makeNewBlock(seqNumber, prevBlockHash);
        log.trace("newBlock {}", newBlock);

        long viewNumber = getCurrentViewNumber(seqNumber);
        PbftMessage prePrepare = new PbftMessage(
                Type.PREPREPA.toString(),
                viewNumber,
                seqNumber,
                newBlock.getHash(),
                null,
                wallet,
                newBlock);
        if (prePrepare.getSignature() == null) {
            return null;
        }

        this.blockChain.getUnConfirmedMsgMap().put(prePrepare.getSignatureHex(), prePrepare);
        this.isPrePrepared = true;

        log.debug("make PrePrepareMsg viewNumber={}", viewNumber);
        log.debug("newBlock [{}] {} ({})", newBlock.getIndex(), newBlock.getHashHex(), newBlock.getAddressHex());

        return prePrepare;
    }

    private long getCurrentViewNumber(long seqNumber) {
        if (this.viewChangeMap.size() >= consensusCount) {
            return ((PbftMessage) this.viewChangeMap.values().toArray()[0]).getViewNumber();
        } else {
            return seqNumber;
        }
    }

    private long getNextActiveValidatorIndex(long index) {
        long validatorCount = this.totalValidatorMap.size();
        log.trace("Before ValidatorIndex: {} {}", index, validatorCount);

        for (long l = index + 1; l <= index + validatorCount; l++) {
            // next validator sequence 0 ~ n
            int validatorSeq = (int) (l % validatorCount);
            PbftClientStub client =
                    (PbftClientStub) this.totalValidatorMap.values().toArray()[validatorSeq];
            if (client.isRunning()) {
                log.trace("NextActiveValidatorIndex: {}", l);
                return l;
            }
        }

        log.error("Cannot get next active validator index!");
        return -1L;
    }

    private Block makeNewBlock(long index, byte[] prevBlockHash) {
        List<Transaction> txs = new ArrayList<>();
        List<TransactionHusk> txHusks = new ArrayList<>(blockChain.getTransactionStore().getUnconfirmedTxs());

        for (TransactionHusk txHusk : txHusks) {
            txs.add(txHusk.getCoreTransaction());
        }

        BlockBody newBlockBody = new BlockBody(txs);
        BlockHeader newBlockHeader = new BlockHeader(
                blockChain.getChain(),
                new byte[8],
                new byte[8],
                prevBlockHash,
                index,
                TimeUtils.time(),
                newBlockBody);
        return new Block(newBlockHeader, wallet, newBlockBody);
    }

    private PbftMessage makePrepareMsg() {
        if (!this.isValidator
                || !this.isActive
                || !this.isSynced
                || !this.isPrePrepared
                || this.isPrepared) {
            return null;
        }

        long seqNumber = this.blockChain.getLastConfirmedBlock().getIndex() + 1;
        PbftMessage prePrepareMsg = getPrePrepareMsg(seqNumber);
        if (prePrepareMsg == null) {
            return null;
        }

        long viewNumber = getCurrentViewNumber(seqNumber);
        PbftMessage prepareMsg = new PbftMessage(
                Type.PREPAREM.toString(),
                viewNumber,
                seqNumber,
                prePrepareMsg.getHash(),
                null,
                wallet,
                null);
        if (prepareMsg.getSignature() == null) {
            return null;
        }

        this.blockChain.getUnConfirmedMsgMap().put(prepareMsg.getSignatureHex(), prepareMsg);
        this.isPrepared = true;

        log.debug("make PrepareMsg "
                + "("
                + prepareMsg.getViewNumber()
                + ")"
                + " ["
                + prepareMsg.getSeqNumber()
                + "] "
                + prepareMsg.getHashHex());

        return prepareMsg;

    }

    private PbftMessage makeCommitMsg() {

        if (!this.isValidator
                || !this.isActive
                || !this.isSynced
                || !this.isPrePrepared
                || !this.isPrepared
                || this.isCommitted) {
            return null;
        }

        long seqNumber = this.blockChain.getLastConfirmedBlock().getIndex() + 1;
        Map<String, PbftMessage> prepareMsgMap = getPrepareMsgMap(seqNumber);
        if (prepareMsgMap.size() < consensusCount) {
            return null;
        }

        long viewNumber = getCurrentViewNumber(seqNumber);
        PbftMessage commitMsg = new PbftMessage(Type.COMMITMS.toString(), viewNumber, seqNumber,
                ((PbftMessage) prepareMsgMap.values().toArray()[0]).getHash(), null, wallet, null);
        if (commitMsg.getSignature() == null) {
            return null;
        }

        this.blockChain.getUnConfirmedMsgMap().put(commitMsg.getSignatureHex(), commitMsg);
        this.isCommitted = true;

        log.debug("make CommitMsg "
                + "("
                + commitMsg.getViewNumber()
                + ")"
                + " ["
                + commitMsg.getSeqNumber()
                + "] "
                + commitMsg.getHashHex());

        return commitMsg;

    }

    private void confirmFinalBlock() {
        int nextCommitCount = 0;

        long index = this.blockChain.getLastConfirmedBlock().getIndex() + 1;
        PbftMessage prePrepareMsg = null;
        Map<String, PbftMessage> prepareMessageMap = new TreeMap<>();
        Map<String, PbftMessage> commitMessageMap = new TreeMap<>();

        for (Map.Entry<String, PbftMessage> entry : blockChain.getUnConfirmedMsgMap().entrySet()) {
            PbftMessage pbftMessage = entry.getValue();
            String key = entry.getKey();
            if (pbftMessage.getSeqNumber() < index) {
                pbftMessage.clear();
                this.blockChain.getUnConfirmedMsgMap().remove(key);
            } else if (pbftMessage.getSeqNumber() == index) {
                prePrepareMsg = getPbftMessage(prePrepareMsg, prepareMessageMap, commitMessageMap, pbftMessage, key);
            } else if (pbftMessage.getSeqNumber() == index + 1
                    && Type.COMMITMS.accept(pbftMessage.getType())) {
                nextCommitCount++;
            }
        }

        if (prePrepareMsg == null) {
            this.failCount++;
        } else if (prepareMessageMap.size() >= consensusCount
                && commitMessageMap.size() >= consensusCount) {
            PbftMessageSet pbftMessageSet = new PbftMessageSet(
                    prePrepareMsg, prepareMessageMap, commitMessageMap, this.viewChangeMap);
            PbftBlock pbftBlock = new PbftBlock(prePrepareMsg.getBlock(), pbftMessageSet);
            confirmedBlock(pbftBlock.clone());
            this.failCount = 0;
            this.viewChangeMap.clear();
            this.isViewChanged = false;
        }

        if (nextCommitCount >= consensusCount) {
            confirmFinalBlock();
        }
    }

    private PbftMessage getPbftMessage(PbftMessage prePrepareMsg, Map<String, PbftMessage> prepareMessageMap,
                                       Map<String, PbftMessage> commitMessageMap, PbftMessage pbftMessage, String key) {
        switch (Type.valueOf(pbftMessage.getType())) {
            case PREPREPA:
                if (prePrepareMsg != null) {
                    // todo: for debugging log
                    log.debug("PrePrepare msg is duplicated.");
                }
                prePrepareMsg = pbftMessage;
                break;
            case PREPAREM:
                prepareMessageMap.put(key, pbftMessage);
                break;
            case COMMITMS:
                commitMessageMap.put(key, pbftMessage);
                break;
            case VIEWCHAN:
                break;
            default:
                log.debug("Invalid message type : {}", pbftMessage.getType());
                break;
        }
        return prePrepareMsg;
    }

    private void confirmedBlock(PbftBlock block) {
        blockChain.getBlockStore().put(block.getHash(), block);
        blockChain.getBlockKeyStore().put(block.getIndex(), block.getHash());
        blockChain.addBlockInternal(block);

        changeLastConfirmedBlock(block);

        log.debug("ConfirmedBlock ({}) [{}] {}",
                block.getPbftMessageSet().getPrePrepare().getViewNumber(),
                blockChain.getLastConfirmedBlock().getIndex(),
                blockChain.getLastConfirmedBlock().getHashHex());
    }

    private PbftMessage makeViewChangeMsg() {
        if (this.failCount < FAIL_COUNT
                || !this.isValidator
                || !this.isActive
                || !this.isSynced
                || this.isViewChanged) {
            return null;
        }

        Block block = this.blockChain.getLastConfirmedBlock().getBlock();
        log.trace("block: {}", block);
        long seqNumber = block.getIndex() + 1;
        long newViewNumber = getNextActiveValidatorIndex(seqNumber);
        if (newViewNumber < 0) {
            return null;
        }

        PbftMessage viewChangeMsg = new PbftMessage(
                VIEWCHAN.toString(),
                newViewNumber,
                seqNumber,
                block.getHash(),
                null,
                wallet,
                null);
        if (viewChangeMsg.getSignature() == null) {
            return null;
        }

        this.blockChain.getUnConfirmedMsgMap().put(viewChangeMsg.getSignatureHex(), viewChangeMsg);
        this.isViewChanged = true;

        log.warn("ViewChanged ({}) -> ({})", seqNumber, newViewNumber);

        return viewChangeMsg;
    }

    private void changeLastConfirmedBlock(PbftBlock block) {
        this.blockChain.setLastConfirmedBlock(block);
        for (String key : this.blockChain.getUnConfirmedMsgMap().keySet()) {
            PbftMessage pbftMessage = this.blockChain.getUnConfirmedMsgMap().get(key);
            if (pbftMessage.getSeqNumber() <= block.getIndex()) {
                pbftMessage.clear();
                this.blockChain.getUnConfirmedMsgMap().remove(key);
            }
        }

        this.isPrePrepared = false;
        this.isPrepared = false;
        this.isCommitted = false;
    }

    private Map<String, PbftMessage> getPrepareMsgMap(long index) {
        Map<String, PbftMessage> prepareMsgMap = new TreeMap<>();
        for (String key : this.blockChain.getUnConfirmedMsgMap().keySet()) {
            PbftMessage pbftMessage = this.blockChain.getUnConfirmedMsgMap().get(key);
            if (pbftMessage.getSeqNumber() == index && PREPAREM.accept(pbftMessage.getType())) {
                prepareMsgMap.put(key, pbftMessage);
            }
        }
        return prepareMsgMap;
    }

    private PbftMessage getPrePrepareMsg(long index) {
        for (String key : this.blockChain.getUnConfirmedMsgMap().keySet()) {
            PbftMessage pbftMessage = this.blockChain.getUnConfirmedMsgMap().get(key);
            if (pbftMessage.getSeqNumber() == index && PREPREPA.accept(pbftMessage.getType())) {
                return pbftMessage;
            }
        }
        return null;
    }

    private void checkPrimary() {
        long blockIndex = this.blockChain.getLastConfirmedBlock().getIndex() + 1;

        int primaryIndex = (int) (checkViewChange(blockIndex) % totalValidatorMap.size());
        String currentPrimaryPubKey = (String) totalValidatorMap.keySet().toArray()[primaryIndex];

        log.debug("Block Index: {}", blockIndex);
        log.debug("Primary Index: {}", primaryIndex);
        log.debug("currentPrimaryPubKey: {}", currentPrimaryPubKey);

        this.isPrimary = currentPrimaryPubKey.equals(this.myNode.getPubKey());
    }

    private long checkViewChange(long index) {
        if (!this.isValidator
                || !this.isActive
                || !this.isSynced) {
            return index;
        }

        Map<String, PbftMessage> viewChangeMsgMap = getViewChangeMsgMap(index);

        log.debug("viewChangeMsgMap size: {}", viewChangeMsgMap.size());
        // todo: check viewNumber
        if (viewChangeMsgMap.size() < consensusCount) {
            return index;
        } else {
            long newViewNumber = ((PbftMessage) viewChangeMsgMap.values().toArray()[0]).getViewNumber();
            this.viewChangeMap.putAll(viewChangeMsgMap);
            return newViewNumber;
        }
    }

    private Map<String, PbftMessage> getViewChangeMsgMap(long index) {
        Map<String, PbftMessage> viewChangeMsgMap = new TreeMap<>();
        for (String key : this.blockChain.getUnConfirmedMsgMap().keySet()) {
            PbftMessage pbftMessage = this.blockChain.getUnConfirmedMsgMap().get(key);
            if (pbftMessage.getSeqNumber() == index && VIEWCHAN.accept(pbftMessage.getType())) {
                viewChangeMsgMap.put(key, pbftMessage);
            }
        }
        return viewChangeMsgMap;
    }

    private void checkNode() {
        for (PbftClientStub client : totalValidatorMap.values()) {
            if (client.isMyClient()) {
                continue;
            }

            long pingTime = System.currentTimeMillis();
            long pongTime = client.pingPongTime(pingTime);

            if (pongTime > 0L) {
                checkNodeStatus(client);
            } else {
                client.setIsRunning(false);
            }
        }

        this.isSynced = true;
        setActiveMode();
    }

    private void checkNodeStatus(PbftClientStub client) {
        PbftStatus pbftStatus = client.exchangePbftStatus(PbftStatus.toProto(getMyNodeStatus()));
        updateStatus(client, pbftStatus);
    }

    private void updateStatus(PbftClientStub client, PbftStatus pbftStatus) {
        if (PbftStatus.verify(pbftStatus)) {
            client.setIsRunning(true);

            if (pbftStatus.getIndex()
                    > this.blockChain.getLastConfirmedBlock().getIndex()) {
                log.debug("this Index: "
                        + this.blockChain.getLastConfirmedBlock().getIndex());
                log.debug("client Index: {}", pbftStatus.getIndex());
                log.debug("client : {}", client.getId());

                this.isSynced = false;
                blockSyncing(client.getPubKey(), pbftStatus.getIndex());
            } else if (pbftStatus.getIndex()
                    == this.blockChain.getLastConfirmedBlock().getIndex()) {
                // update unConfirm pbftMessage
                pbftStatus.getUnConfirmedPbftMessageMap().values().forEach(this::updateUnconfirmedMsg);
            }
        } else {
            client.setIsRunning(false);
        }
    }

    private void blockSyncing(String pubKey, long index) {
        PbftClientStub client = totalValidatorMap.get(pubKey);
        PbftBlock pbftBlock;
        if (client.isRunning()) {
            List<PbftBlock> pbftBlockList = client.getBlockList(blockChain.getLastConfirmedBlock().getIndex());

            log.debug("node: {}", client.getId());
            log.debug("index: {}", index);
            log.debug("blockList size: {}", pbftBlockList.size());

            if (pbftBlockList.isEmpty()) {
                return;
            }

            int i = 0;
            for (; i < pbftBlockList.size(); i++) {
                pbftBlock = pbftBlockList.get(i);
                if (!PbftBlock.verify(pbftBlock)) {
                    log.warn("Verify Fail");
                    return;
                }
                blockChain.getBlockStore().put(pbftBlock.getHash(), pbftBlock);
                blockChain.getBlockKeyStore().put(pbftBlock.getIndex(), pbftBlock.getHash());
                blockChain.addBlockInternal(pbftBlock);
            }
            pbftBlock = pbftBlockList.get(i - 1);
            changeLastConfirmedBlock(pbftBlock);
        }

        if (this.blockChain.getLastConfirmedBlock().getIndex() < index) {
            blockSyncing(pubKey, index);
        }
    }

    void updateUnconfirmedMsg(PbftMessage newPbftMessage) {
        this.blockChain.getUnConfirmedMsgMap()
                .put(newPbftMessage.getSignatureHex(), newPbftMessage);

        if (newPbftMessage.getSeqNumber() == this.blockChain.getLastConfirmedBlock().getIndex() + 1
                && PREPREPA.accept(newPbftMessage.getType())) {
            this.isPrePrepared = true;
        }
    }

    PbftStatus getMyNodeStatus() {
        long index = this.blockChain.getLastConfirmedBlock().getIndex();
        Map<String, PbftMessage> pbftMessageMap = new TreeMap<>();
        ByteArrayOutputStream pbftMessageBytes = new ByteArrayOutputStream();
        for (String key : this.blockChain.getUnConfirmedMsgMap().keySet()) {
            PbftMessage pbftMessage = this.blockChain.getUnConfirmedMsgMap().get(key);
            if (pbftMessage.getSeqNumber() == index + 1) {
                pbftMessageMap.put(key, pbftMessage);
                try {
                    pbftMessageBytes.write(pbftMessage.toBinary());
                } catch (IOException e) {
                    log.debug(e.getMessage());
                }
            }
        }
        long timestamp = TimeUtils.time();

        return new PbftStatus(index, pbftMessageMap, timestamp, wallet);
    }

    private void printInitInfo() {
        String pubKey = Hex.toHexString(wallet.getPubicKey());
        log.info("Node Started");
        log.info("wallet address: {}", wallet.getHexAddress());
        log.info("wallet pubKey: {}", pubKey);
        log.info("isValidator: {}", isValidator);
    }

    private void initTotalValidator() {
        JsonObject consensus = blockChain.getConsensus();
        log.debug("consensus={}", consensus);

        Set<Map.Entry<String, JsonElement>> entrySet = consensus.get("validator").getAsJsonObject().entrySet();
        for (Map.Entry<String, JsonElement> entry : entrySet) {
            Peer peer = Peer.valueOf(entry.getKey(),
                    entry.getValue().getAsJsonObject().get("host").getAsString(),
                    entry.getValue().getAsJsonObject().get("port").getAsInt());
            if (myNode.getPubKey().equals(peer.getPubKey().toString())) {
                totalValidatorMap.put(myNode.getPubKey(), myNode);
            } else {
                PbftClientStub client = new PbftClientStub(peer);
                totalValidatorMap.put(client.getPubKey(), client);
            }
        }
    }

    private PbftClientStub initMyNode() {
        PbftClientStub stub = new PbftClientStub(blockChain.getOwner());
        stub.setMyClient(true);
        stub.setIsRunning(true);

        return stub;
    }

    private boolean isValidator() {
        log.debug("MyNode ID: {}",  myNode.getId());
        return totalValidatorMap.containsKey(this.myNode.getPubKey());
    }

    private void setActiveMode() {
        int runningNodeCount = getActiveNodeCount();
        if (runningNodeCount >= consensusCount) {
            if (!this.isActive) {
                this.isActive = true;
                log.info("Node is activated.");
            }
        } else {
            if (this.isActive) {
                this.isActive = false;
                log.info("Node is deactivated.");
            }
        }

        log.debug("running node: {}", runningNodeCount);
    }

    private int getActiveNodeCount() {
        int count = 0;
        for (PbftClientStub client : totalValidatorMap.values()) {
            if (client.isRunning()) {
                count++;
            }
        }
        return count;
    }

    // todo: check security
    ReentrantLock getLock() {
        return lock;
    }
}
