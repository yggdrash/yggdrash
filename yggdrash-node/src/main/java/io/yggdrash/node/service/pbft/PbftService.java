package io.yggdrash.node.service.pbft;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.common.utils.Sha3Hash;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockBody;
import io.yggdrash.core.blockchain.BlockHeader;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.blockchain.pbft.PbftBlock;
import io.yggdrash.core.blockchain.pbft.PbftMessage;
import io.yggdrash.core.blockchain.pbft.PbftMessageSet;
import io.yggdrash.core.blockchain.pbft.PbftStatus;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Wallet;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
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

@Profile("validator")
@Service
@EnableScheduling
public class PbftService implements CommandLineRunner {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(PbftService.class);
    private static final int FAIL_COUNT = 2;

    private final boolean isValidator;
    private final int bftCount;
    private final int consensusCount;

    private final Wallet wallet;
    private final PbftBlockChain blockChain;
    private final BranchGroup branchGroup;

    private final PbftClientStub myNode;
    private final Map<String, PbftClientStub> totalValidatorMap;

    private final ReentrantLock lock = new ReentrantLock();

    private boolean isActive;
    private boolean isSynced;
    private boolean isPrePrepared;
    private boolean isPrepared;
    private boolean isCommitted;
    private boolean isViewChanged;

    private boolean isPrimary;
    private String currentPrimaryPubKey;

    private int failCount;
    private final Map<String, PbftMessage> viewChangeMap = new TreeMap<>();

    @Autowired
    public PbftService(Wallet wallet, PbftBlockChain pbftBlockChain, BranchGroup branchGroup) {
        this.wallet = wallet;
        this.blockChain = pbftBlockChain;
        this.branchGroup = branchGroup;
        this.myNode = initMyNode();
        this.totalValidatorMap = initTotalValidator();
        this.isValidator = initValidator();
        if (totalValidatorMap != null) {
            this.bftCount = (totalValidatorMap.size() - 1) / 3;
            this.consensusCount = bftCount * 2 + 1;
        } else {
            this.consensusCount = 0;
            throw new NotValidateException();
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
    @Profile("validator")
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
        log.trace("loggingStatus");
        log.debug("failCount= " + this.failCount);
        log.debug("isAcitve=" + this.isActive);
        log.debug("isSynced=" + this.isSynced);
        log.debug("isPrePrepared= " + this.isPrePrepared);
        log.debug("isPrepared= " + this.isPrepared);
        log.debug("isCommitted= " + this.isCommitted);
        log.debug("isViewChanged= " + this.isViewChanged);

        PbftBlock lastBlock = this.blockChain.getLastConfirmedBlock();
        try {
            log.info("PbftBlock "
                    + "("
                    + lastBlock.getPbftMessageSet().getPrePrepare().getViewNumber()
                    + ") "
                    + "["
                    + lastBlock.getIndex()
                    + "]"
                    + lastBlock.getHashHex()
                    + " ("
                    + lastBlock.getPbftMessageSet().getPrepareMap().size()
                    + ")"
                    + " ("
                    + lastBlock.getPbftMessageSet().getCommitMap().size()
                    + ")"
                    + " ("
                    + lastBlock.getPbftMessageSet().getViewChangeMap().size()
                    + ")"
                    + " ("
                    + lastBlock.getBlock().getAddressHex()
                    + ")");
        } catch (Exception e) {
            log.debug(e.getMessage());
        }

        log.debug("unConfirmedMsgMap size= " + this.blockChain.getUnConfirmedMsgMap().size());
        log.debug("TxStore unConfirmed Tx.size= "
                + this.blockChain.getTransactionStore().getUnconfirmedTxs().size());
        log.debug("");
    }

    private void multicastMessage(PbftMessage message) {
        for (String key : totalValidatorMap.keySet()) {
            PbftClientStub client = totalValidatorMap.get(key);
            if (client.isMyclient()) {
                continue;
            }
            if (client.isRunning()) {
                try {
                    client.multicastPbftMessage(PbftMessage.toProto(message));
                } catch (Exception e) {
                    log.debug("multicast exception: " + e.getMessage());
                    log.debug("client: " + client.getId());
                    log.debug("message: " + message.toString());
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
        log.trace("newBlock" + newBlock.toString());

        long viewNumber = getCurrentViewNumber(seqNumber);
        PbftMessage prePrepare = new PbftMessage(
                "PREPREPA",
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

        log.debug("make PrePrepareMsg "
                + "("
                + viewNumber
                + ") "
                + "["
                + newBlock.getIndex()
                + "] "
                + newBlock.getHashHex()
                + " ("
                + newBlock.getAddressHex()
                + ")");

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
        log.trace("Before ValidatorIndex: " + index + " " + validatorCount);

        for (long l = index + 1; l <= index + validatorCount; l++) {
            // next validator sequence 0 ~ n
            int validatorSeq = (int) (l % validatorCount);
            PbftClientStub client =
                    (PbftClientStub) this.totalValidatorMap.values().toArray()[validatorSeq];
            if (client.isRunning()) {
                log.trace("NextActiveValidatorIndex: " + l);
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
                "PREPAREM",
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
        if (prepareMsgMap == null || prepareMsgMap.size() < consensusCount) {
            return null;
        }

        long viewNumber = getCurrentViewNumber(seqNumber);
        PbftMessage commitMsg = new PbftMessage("COMMITMS", viewNumber, seqNumber,
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

        for (String key : this.blockChain.getUnConfirmedMsgMap().keySet()) {
            PbftMessage pbftMessage = this.blockChain.getUnConfirmedMsgMap().get(key);
            if (pbftMessage == null || pbftMessage.getSeqNumber() < index) {
                pbftMessage.clear();
                this.blockChain.getUnConfirmedMsgMap().remove(key);
            } else if (pbftMessage.getSeqNumber() == index) {
                switch (pbftMessage.getType()) {
                    case "PREPREPA":
                        if (prePrepareMsg != null) {
                            // todo: for debugging log
                            log.debug("PrePrepare msg is duplicated.");
                        }
                        prePrepareMsg = pbftMessage;
                        break;
                    case "PREPAREM":
                        prepareMessageMap.put(key, pbftMessage);
                        break;
                    case "COMMITMS":
                        commitMessageMap.put(key, pbftMessage);
                        break;
                    case "VIEWCHAN":
                        break;
                    default:
                        log.debug("Invalid message type :" + pbftMessage.getType());
                        break;
                }
            } else if (pbftMessage.getSeqNumber() == index + 1
                    && pbftMessage.getType().equals("COMMITMS")) {
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

    private void confirmedBlock(PbftBlock block) {
        this.blockChain.getBlockStore().put(block.getHash(), block);
        this.blockChain.getBlockKeyStore().put(block.getIndex(), block.getHash());

        this.branchGroup.addBlock(new BlockHusk(block.getBlock()), true);

        changeLastConfirmedBlock(block);

        log.debug("ConfirmedBlock "
                + "("
                + block.getPbftMessageSet().getPrePrepare().getViewNumber()
                + ") "
                + "["
                + this.blockChain.getLastConfirmedBlock().getIndex()
                + "] "
                + this.blockChain.getLastConfirmedBlock().getHashHex());
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
        log.trace("block" + block.toString());
        long seqNumber = block.getIndex() + 1;
        long newViewNumber = getNextActiveValidatorIndex(seqNumber);
        if (newViewNumber < 0) {
            return null;
        }

        PbftMessage viewChangeMsg = new PbftMessage(
                "VIEWCHAN",
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

        log.warn("ViewChanged"
                + " ("
                + seqNumber
                + ") ->"
                + " ("
                + newViewNumber
                + ")");

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
            if (pbftMessage.getSeqNumber() == index
                    && pbftMessage.getType().equals("PREPAREM")) {
                prepareMsgMap.put(key, pbftMessage);
            }
        }
        return prepareMsgMap;
    }

    private PbftMessage getPrePrepareMsg(long index) {
        for (String key : this.blockChain.getUnConfirmedMsgMap().keySet()) {
            PbftMessage pbftMessage = this.blockChain.getUnConfirmedMsgMap().get(key);
            if (pbftMessage.getSeqNumber() == index
                    && pbftMessage.getType().equals("PREPREPA")) {
                return pbftMessage;
            }
        }
        return null;
    }

    private void checkPrimary() {
        long blockIndex = this.blockChain.getLastConfirmedBlock().getIndex() + 1;

        int primaryIndex = (int) (checkViewChange(blockIndex) % totalValidatorMap.size());
        currentPrimaryPubKey = (String) totalValidatorMap.keySet().toArray()[primaryIndex];

        log.debug("Block Index: " + blockIndex);
        log.debug("Primary Index: " + primaryIndex);
        log.debug("currentPrimaryPubKey: " + currentPrimaryPubKey);

        this.isPrimary = currentPrimaryPubKey.equals(this.myNode.getPubKey());
    }

    private long checkViewChange(long index) {
        if (!this.isValidator
                || !this.isActive
                || !this.isSynced) {
            return index;
        }

        Map<String, PbftMessage> viewChangeMsgMap = getViewChangeMsgMap(index);

        log.debug("viewChangeMsgMap size: " + viewChangeMsgMap.size());
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
            if (pbftMessage.getSeqNumber() == index
                    && pbftMessage.getType().equals("VIEWCHAN")) {
                viewChangeMsgMap.put(key, pbftMessage);
            }
        }
        return viewChangeMsgMap;
    }

    private void checkNode() {
        for (String key : totalValidatorMap.keySet()) {
            PbftClientStub client = totalValidatorMap.get(key);
            if (client.isMyclient()) {
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
                log.debug("client Index: " + pbftStatus.getIndex());
                log.debug("client : " + client.getId());

                this.isSynced = false;
                blockSyncing(client.getPubKey(), pbftStatus.getIndex());
            } else if (pbftStatus.getIndex()
                    == this.blockChain.getLastConfirmedBlock().getIndex()) {
                // update unConfirm pbftMessage
                updateUnconfirmedMsgMap(pbftStatus.getUnConfirmedPbftMessageMap());
            }
        } else {
            client.setIsRunning(false);
        }
    }

    private void blockSyncing(String pubKey, long index) {
        PbftClientStub client = totalValidatorMap.get(pubKey);
        PbftBlock pbftBlock;
        if (client.isRunning()) {
            List<PbftBlock> pbftBlockList = new ArrayList<>(client.getBlockList(
                    this.blockChain.getLastConfirmedBlock().getIndex()));

            log.debug("node: " + client.getId());
            log.debug("index: " + index);
            log.debug("blockList size: " + pbftBlockList.size());

            if (pbftBlockList.size() == 0) {
                return;
            }

            int i = 0;
            for (; i < pbftBlockList.size(); i++) {
                pbftBlock = pbftBlockList.get(i);
                if (!PbftBlock.verify(pbftBlock)) {
                    log.warn("Verify Fail");
                    return;
                }
                this.blockChain.getBlockStore().put(pbftBlock.getHash(), pbftBlock);
                this.blockChain.getBlockKeyStore()
                        .put(pbftBlock.getIndex(), pbftBlock.getHash());

                this.branchGroup.addBlock(new BlockHusk(pbftBlock.getBlock()), true);
            }
            pbftBlock = pbftBlockList.get(i - 1);
            changeLastConfirmedBlock(pbftBlock);
        }

        if (this.blockChain.getLastConfirmedBlock().getIndex() < index) {
            blockSyncing(pubKey, index);
        }
    }

    public void updateUnconfirmedMsg(PbftMessage newPbftMessage) {
        this.blockChain.getUnConfirmedMsgMap()
                .put(newPbftMessage.getSignatureHex(), newPbftMessage);

        if (newPbftMessage.getType().equals("PREPREPA")
                && newPbftMessage.getSeqNumber()
                == this.blockChain.getLastConfirmedBlock().getIndex() + 1) {
            this.isPrePrepared = true;
        }
    }

    public void updateUnconfirmedMsgMap(Map<String, PbftMessage> newPbftMessageMap) {
        for (String key : newPbftMessageMap.keySet()) {
            PbftMessage newPbftMessage = newPbftMessageMap.get(key);
            updateUnconfirmedMsg(newPbftMessage);
        }
    }

    public PbftStatus getMyNodeStatus() {
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
        log.info("Node Started");
        log.info("wallet address: " + wallet.getHexAddress());
        log.info("wallet pubKey: " + Hex.toHexString(wallet.getPubicKey()));
        log.info("isValidator: " + this.isValidator);
    }

    private TreeMap<String, PbftClientStub> initTotalValidator() {
        JsonObject validatorJsonObject =
                branchGroup.getBranch(new BranchId(new Sha3Hash(blockChain.getChain(), true)))
                        .getBranch().getConsensus();
        TreeMap<String, PbftClientStub> nodeMap = new TreeMap<>();

        Set<Map.Entry<String, JsonElement>> entrySet =
                validatorJsonObject.get("validator").getAsJsonObject().entrySet();
        for (Map.Entry<String, JsonElement> entry : entrySet) {
            PbftClientStub client = new PbftClientStub(entry.getKey(),
                    entry.getValue().getAsJsonObject().get("host").getAsString(),
                    entry.getValue().getAsJsonObject().get("port").getAsInt());
            if (client.getId().equals(myNode.getId())) {
                nodeMap.put(myNode.getPubKey(), myNode);
            } else {
                nodeMap.put(client.getPubKey(), client);
            }
        }

        log.debug("isValidator" + validatorJsonObject.toString());
        return nodeMap;
    }

    private PbftClientStub initMyNode() {
        PbftClientStub client = new PbftClientStub(
                wallet.getPubicKeyHex().substring(2),
                this.blockChain.getHost(),
                this.blockChain.getPort());

        client.setMyclient(true);
        client.setIsRunning(true);

        return client;
    }

    private boolean initValidator() {
        log.debug("MyNode ID: " + this.myNode.getId());
        return totalValidatorMap.containsKey(this.myNode.getPubKey());
    }

    private List<String> getActiveNodeList() {
        List<String> activeNodeList = new ArrayList<>();
        for (String key : totalValidatorMap.keySet()) {
            PbftClientStub client = totalValidatorMap.get(key);
            if (client.isMyclient()) {
                continue;
            }
            if (client.isRunning()) {
                activeNodeList.add(client.getId());
            }
        }
        return activeNodeList;
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

        log.debug("running node: " + runningNodeCount);
    }

    private int getActiveNodeCount() {
        int count = 0;
        for (String key : totalValidatorMap.keySet()) {
            if (totalValidatorMap.get(key).isRunning()) {
                count++;
            }
        }
        return count;
    }

    // todo: check security
    public ReentrantLock getLock() {
        return lock;
    }
}
