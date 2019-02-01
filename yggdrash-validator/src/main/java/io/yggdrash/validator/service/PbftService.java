package io.yggdrash.validator.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.common.util.ByteUtil;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockBody;
import io.yggdrash.core.blockchain.BlockHeader;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.data.PbftBlock;
import io.yggdrash.validator.data.PbftBlockChain;
import io.yggdrash.validator.data.PbftStatus;
import io.yggdrash.validator.data.pbft.PbftMessage;
import io.yggdrash.validator.data.pbft.PbftMessageSet;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

import static io.yggdrash.common.util.Utils.sleep;

@Service
@EnableScheduling
@ConditionalOnProperty(name = "yggdrash.validator.consensus.algorithm", havingValue = "pbft")
public class PbftService implements CommandLineRunner {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(PbftService.class);

    private final boolean isValidator;
    private final int bftCount;
    private final int consenusCount;

    private final Wallet wallet;
    private final PbftBlockChain blockChain;

    private final PbftClientStub myNode;
    private final TreeMap<String, PbftClientStub> totalValidatorMap;

    private boolean isActive;
    private boolean isSynced;
    private boolean isPrePrepared;
    private boolean isPrepared;
    private boolean isCommited;

    private boolean isPrimary;
    private String currentPrimaryPubKey;

    private int failCount;

    private ReentrantLock lock = new ReentrantLock();

    @Autowired
    public PbftService(Wallet wallet, PbftBlockChain blockChain) {
        this.wallet = wallet;
        this.blockChain = blockChain;
        this.myNode = initMyNode();
        this.totalValidatorMap = initTotalValidator();
        this.isValidator = initValidator();
        if (totalValidatorMap != null) {
            this.bftCount = (totalValidatorMap.size() - 1) / 3;
            this.consenusCount = bftCount * 2 + 1;
        } else {
            this.consenusCount = 0;
            throw new NotValidateException();
        }
        this.isActive = false;
        this.isSynced = false;
        this.isPrePrepared = false;
        this.isPrepared = false;
        this.isCommited = false;

        this.failCount = 0;
    }

    @Override
    public void run(String... args) {
        printInitInfo();
    }

    @Scheduled(cron = "*/5 * * * * *")
    public void mainScheduler() {

        checkNode();

        checkPrimary();

        // make PrePrepare msg
        PbftMessage prePrepareMsg = makePrePrepareMsg();
        if (prePrepareMsg != null) {
            multicastMessage(prePrepareMsg);
        }

        sleep(500);

        // make Prepare msg
        PbftMessage prepareMsg = makePrepareMsg();
        if (prepareMsg != null) {
            multicastMessage(prepareMsg);
        }

        sleep(500);

        // make commit msg
        PbftMessage commitMsg = makeCommitMsg();
        if (commitMsg != null) {
            multicastMessage(commitMsg);
        }

        sleep(500);

        confirmFinalBlock();

        loggingStatus();

        PbftMessage viewChangeMsg = makeViewChangeMsg();
        if (viewChangeMsg != null) {
            multicastMessage(viewChangeMsg);
        }

        log.info("");
    }

    private void loggingStatus() {
        log.trace("loggingStatus");
        log.debug("isAcitve=" + this.isActive);
        log.debug("isSynced=" + this.isSynced);
        log.debug("isPrePrepared=" + this.isPrePrepared);
        log.debug("isPrepared=" + this.isPrepared);
        log.debug("isCommited=" + this.isCommited);

        PbftBlock lastBlock = this.blockChain.getLastConfirmedBlock();

        log.info("PbftBlock ["
                + lastBlock.getIndex()
                + "]"
                + " "
                + lastBlock.getHashHex()
                + " ("
                + lastBlock.getBlock().getAddressHex()
                + ")");
        if (lastBlock.getPbftMessageSet() != null) {
            log.debug("PbftMessageSet= {} {}",
                    lastBlock.getPbftMessageSet().getPrepareMap().size(),
                    lastBlock.getPbftMessageSet().getCommitMap().size());
        }

        log.debug("unConfirmedMsgMap size= " + this.blockChain.getUnConfirmedMsgMap().size());

        log.debug("blockStore size= " + this.blockChain.getBlockStore().size());
        log.debug("blockKeyStore size= " + this.blockChain.getBlockKeyStore().size());

        if (!this.blockChain.TEST_NONE_TXSTORE) {
            log.debug("TxStore unConfirmed Tx.size= "
                    + this.blockChain.getTransactionStore().getUnconfirmedTxs().size());
        }
    }

    private void multicastBlock(PbftBlock block) {
        for (String key : totalValidatorMap.keySet()) {
            PbftClientStub client = totalValidatorMap.get(key);
            if (client.isMyclient()) {
                continue;
            }
            if (client.isRunning()) {
                try {
                    client.multicastPbftBlock(PbftBlock.toProto(block));
                } catch (Exception e) {
                    log.debug("multicast exception: " + e.getMessage());
                    log.debug("client: " + client.getId());
                    log.debug("block: " + block.getHashHex());
                    // continue
                }
            }
        }
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

        long index = this.blockChain.getLastConfirmedBlock().getIndex() + 1;
        byte[] prevBlockHash = this.blockChain.getLastConfirmedBlock().getHash();

        Block newBlock = makeNewBlock(index, prevBlockHash);
        log.trace("newBlock" + newBlock.toString());

        PbftMessage prePrepare = new PbftMessage("PREPREPA", index, index, newBlock.getHash(), null, wallet, newBlock);
        if (prePrepare == null) {
            return null;
        }

        this.blockChain.getUnConfirmedMsgMap().put(prePrepare.getSignatureHex(), prePrepare);
        this.isPrePrepared = true;

        log.debug("make PrePrepareMsg"
                + " ["
                + newBlock.getIndex()
                + "] "
                + newBlock.getHashHex()
                + " ("
                + newBlock.getAddressHex()
                + ")");

        return prePrepare;
    }


    private PbftMessage makeViewChangeMsg() {
        if (this.failCount < 2
                || !this.isValidator
                || !this.isActive
                || !this.isSynced) {
            return null;
        }

        Block block = this.blockChain.getLastConfirmedBlock().getBlock();
        log.trace("block" + block.toString());
        long index = block.getIndex() + 1;
        long newViewIndex = index + getActiveNextValidatorIndex(index);

        PbftMessage viewChange = new PbftMessage(
                "VIEWCHAN",
                newViewIndex,
                index,
                block.getHash(),
                null,
                wallet,
                block);
        if (viewChange == null) {
            return null;
        }

        this.blockChain.getUnConfirmedMsgMap().put(viewChange.getSignatureHex(), viewChange);
        this.isPrePrepared = true;

        log.debug("make ViewChangeMsg"
                + " ("
                + newViewIndex
                + ")"
                + " ("
                + index
                + ") "
                + "["
                + block.getIndex()
                + "] "
                + block.getHashHex()
                + " ("
                + block.getAddressHex()
                + ")");

        return viewChange;
    }

    private long getActiveNextValidatorIndex(long index) {
        log.trace("Before ActiveNextValidatorIndex: " + index);
        int validatorCount = this.totalValidatorMap.size();
        int offSet = ((int) index + 1) % validatorCount;
        for (int i = 0; i < this.totalValidatorMap.size(); i++) {
            int seq = (i + offSet) % validatorCount;
            PbftClientStub client =
                    (PbftClientStub) this.totalValidatorMap.values().toArray()[seq];
            if (client.isRunning()) {
                if (offSet < seq) {
                    log.trace("ActiveNextValidatorIndex: " + ((seq - offSet + 1)) % validatorCount);
                    return (long) ((seq - offSet + 1) % validatorCount);
                } else {
                    log.trace("ActiveNextValidatorIndex: "
                            + ((validatorCount + seq - offSet + 1) % validatorCount));
                    return (long) ((validatorCount + seq - offSet + 1) % validatorCount);
                }
            }
        }
        log.error("ActiveNextValidatorIndex cannot get next index!");
        return -1L;
    }

    private Block makeNewBlock(long index, byte[] prevBlockHash) {
        List<Transaction> txs = new ArrayList<>();
        List<TransactionHusk> txHusks = new ArrayList<>();
        if (!this.blockChain.TEST_NONE_TXSTORE) {
            txHusks.addAll(blockChain.getTransactionStore().getUnconfirmedTxs());
        }

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

        long index = this.blockChain.getLastConfirmedBlock().getIndex() + 1;
        PbftMessage prePrepareMsg = getPrePrepareMsg(index);
        if (prePrepareMsg == null) {
            return null;
        }

        PbftMessage prepareMsg = new PbftMessage("PREPAREM", index, index, prePrepareMsg.getHash(), null, wallet, null);
        if (prepareMsg.getSignature() == null) {
            return null;
        }

        this.blockChain.getUnConfirmedMsgMap().put(prepareMsg.getSignatureHex(), prepareMsg);
        this.isPrepared = true;

        log.debug("make PrepareMsg"
                + " ["
                + prepareMsg.getViewNumber()
                + "]"
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
                || this.isCommited) {
            return null;
        }

        long index = this.blockChain.getLastConfirmedBlock().getIndex() + 1;
        Map<String, PbftMessage> prepareMsgMap = getPrepareMsgMap(index);
        if (prepareMsgMap == null || prepareMsgMap.size() < consenusCount) {
            return null;
        }

        PbftMessage commitMsg = new PbftMessage("COMMITMS", index, index,
                ((PbftMessage) prepareMsgMap.values().toArray()[0]).getHash(), null, wallet, null);
        if (commitMsg.getSignature() == null) {
            return null;
        }

        this.blockChain.getUnConfirmedMsgMap().put(commitMsg.getSignatureHex(), commitMsg);
        this.isCommited = true;

        log.debug("make CommitMsg"
                + " ["
                + commitMsg.getViewNumber()
                + "]"
                + " ["
                + commitMsg.getSeqNumber()
                + "] "
                + commitMsg.getHashHex());

        return commitMsg;

    }

    private void confirmFinalBlock() {
        int nextCommitCount = 0;

        long index = this.blockChain.getLastConfirmedBlock().getIndex() + 1;
        PbftMessage prePrepare = null;
        Map<String, PbftMessage> prepareMap = new TreeMap<>();
        Map<String, PbftMessage> comitMap = new TreeMap<>();

        for (String key : this.blockChain.getUnConfirmedMsgMap().keySet()) {
            PbftMessage pbftMessage = this.blockChain.getUnConfirmedMsgMap().get(key);
            if (pbftMessage == null || pbftMessage.getSeqNumber() < index) {
                this.blockChain.getUnConfirmedMsgMap().remove(key);
                continue;
            } else if (pbftMessage.getSeqNumber() == index) {
                switch (pbftMessage.getType()) {
                    case "PREPREPA":
                        if (prePrepare != null) {
                            // todo: for debugging log
                            log.debug("PrePrepare msg is duplicated.");
                        }
                        prePrepare = pbftMessage;
                        break;
                    case "PREPAREM":
                        prepareMap.put(key, pbftMessage);
                        break;
                    case "COMMITMS":
                        comitMap.put(key, pbftMessage);
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

        if (prePrepare != null
                && prepareMap.size() >= consenusCount
                && comitMap.size() >= consenusCount) {
            PbftMessageSet pbftMessageSet = new PbftMessageSet(prePrepare, prepareMap, comitMap);
            PbftBlock pbftBlock = new PbftBlock(prePrepare.getBlock(), pbftMessageSet);
            confirmedBlock(pbftBlock);
            this.failCount = 0;
        } else {
            this.failCount++;
        }

        if (nextCommitCount >= consenusCount) {
            confirmFinalBlock();
        }
    }

    private void confirmedBlock(PbftBlock block) {
        this.blockChain.getBlockStore().put(block.getHash(), block);
        this.blockChain.getBlockKeyStore().put(block.getIndex(), block.getHash());

        changeLastConfirmedBlock(block);

        log.debug("ConfirmedBlock "
                + "["
                + this.blockChain.getLastConfirmedBlock().getIndex()
                + "] "
                + this.blockChain.getLastConfirmedBlock().getHashHex()
                + " ("
                + this.blockChain.getLastConfirmedBlock().getPbftMessageSet().getPrepareMap().size()
                + ", "
                + this.blockChain.getLastConfirmedBlock().getPbftMessageSet().getCommitMap().size()
                + ")");
    }

    private void changeLastConfirmedBlock(PbftBlock block) {
        this.blockChain.setLastConfirmedBlock(block);
        for (String key : this.blockChain.getUnConfirmedMsgMap().keySet()) {
            PbftMessage pbftMessage = this.blockChain.getUnConfirmedMsgMap().get(key);
            if (pbftMessage.getSeqNumber() <= block.getIndex()) {
                this.blockChain.getUnConfirmedMsgMap().remove(key);
            }
        }

        this.isPrePrepared = false;
        this.isPrepared = false;
        this.isCommited = false;
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
        int primaryIndex = (int) (blockIndex % totalValidatorMap.size());
        currentPrimaryPubKey = (String) totalValidatorMap.keySet().toArray()[primaryIndex];

        if (currentPrimaryPubKey.equals(this.myNode.getPubKey())) {
            this.isPrimary = true;
        } else {
            this.isPrimary = false;
        }
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
                // update unconfirm pbftMessage
                updateUnconfirmedMsgMap(pbftStatus.getPbftMessageMap());
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
            }
            pbftBlock = pbftBlockList.get(i - 1);
            changeLastConfirmedBlock(pbftBlock);
        }

        if (this.blockChain.getLastConfirmedBlock().getIndex() < index) {
            blockSyncing(pubKey, index);
        }
    }

    public void updateUnconfirmedMsg(PbftMessage newPbftMessage) {
        String key = newPbftMessage.getSignatureHex();
        if (this.blockChain.getUnConfirmedMsgMap().containsKey(key)) {
            return;
        }
        this.blockChain.getUnConfirmedMsgMap().put(key, newPbftMessage);
        if (newPbftMessage.getType().equals("PREPREPA")
                && newPbftMessage.getSeqNumber() ==
                this.blockChain.getLastConfirmedBlock().getIndex() + 1) {
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
                    continue;
                }
            }
        }
        long timestamp = TimeUtils.time();

        return new PbftStatus(index,
                pbftMessageMap,
                timestamp,
                wallet.sign(ByteUtil.merge(ByteUtil.longToBytes(index),
                        pbftMessageBytes.toByteArray(),
                        ByteUtil.longToBytes(timestamp))));
    }

    private void printInitInfo() {
        log.info("Node Started");
        log.info("wallet address: " + wallet.getHexAddress());
        log.info("wallet pubKey: " + Hex.toHexString(wallet.getPubicKey()));
        log.info("isValidator: " + this.isValidator);
    }

    private TreeMap<String, PbftClientStub> initTotalValidator() {
        String jsonString;
        ClassPathResource cpr = new ClassPathResource("validator.json");
        try {
            byte[] bdata = FileCopyUtils.copyToByteArray(cpr.getInputStream());
            jsonString = new String(bdata, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.debug("Error validator.json");
            return null;
        }

        JsonObject validatorJsonObject = new Gson().fromJson(jsonString, JsonObject.class);
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
                InetAddress.getLoopbackAddress().getHostAddress(),
                Integer.parseInt(System.getProperty("grpc.port")));

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
        if (runningNodeCount >= consenusCount) {
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
