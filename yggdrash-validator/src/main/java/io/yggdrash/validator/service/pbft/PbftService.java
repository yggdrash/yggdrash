package io.yggdrash.validator.service.pbft;

import com.typesafe.config.ConfigException;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockBody;
import io.yggdrash.core.blockchain.BlockHeader;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.data.ConsensusBlockChain;
import io.yggdrash.validator.data.pbft.PbftBlock;
import io.yggdrash.validator.data.pbft.PbftBlockChain;
import io.yggdrash.validator.data.pbft.PbftMessage;
import io.yggdrash.validator.data.pbft.PbftMessageSet;
import io.yggdrash.validator.data.pbft.PbftStatus;
import io.yggdrash.validator.service.ConsensusService;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

public class PbftService implements ConsensusService {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(PbftService.class);

    private static final int FAIL_COUNT = 2;

    private final boolean isValidator;
    private final int bftCount;
    private final int consensusCount;

    private final DefaultConfig defaultConfig;
    private final Wallet wallet;
    private final PbftBlockChain blockChain;

    private final PbftClientStub myNode;
    private final Map<String, PbftClientStub> totalValidatorMap;
    private final Map<String, PbftClientStub> proxyNodeMap;

    private final ReentrantLock lock = new ReentrantLock();

    private boolean isActive;
    private boolean isSynced;
    private boolean isPrePrepared;
    private boolean isPrepared;
    private boolean isCommitted;
    private boolean isViewChanged;

    private boolean isPrimary;
    private long viewNumber;
    private long seqNumber;
    private String currentPrimaryAddr;

    private int failCount;

    private final String grpcHost;
    private final int grpcPort;

    public PbftService(Wallet wallet,
                       ConsensusBlockChain blockChain,
                       DefaultConfig defaultConfig,
                       String grpcHost,
                       int grpcPort) {
        this.wallet = wallet;
        this.blockChain = (PbftBlockChain) blockChain;
        this.defaultConfig = defaultConfig;
        this.grpcHost = grpcHost;
        this.grpcPort = grpcPort;

        this.myNode = initMyNode();
        this.totalValidatorMap = initTotalValidator();
        this.proxyNodeMap = initProxyNode();
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

        this.viewNumber = this.blockChain.getLastConfirmedBlock().getIndex() + 1;
        this.seqNumber = this.blockChain.getLastConfirmedBlock().getIndex() + 1;

        printInitInfo();
    }

    @Override
    public void run() {
        mainScheduler();
    }

    public void mainScheduler() {
        if (!isValidator) {
            log.debug("Node is not validator.");
            return;
        }

        loggingStatus();

        checkNode();

        if (!isActive) {
            log.debug("Validators are not activate.");
            return;
        }

        lock.lock();
        PbftMessage viewChangeMsg = makeViewChangeMsg();
        lock.unlock();
        if (viewChangeMsg != null) {
            multicastMessage(viewChangeMsg);
            if (!waitingForMessage("VIEWCHAN")) {
                log.debug("VIEWCHAN messages are not enough.");
            }
        }

        lock.lock();
        checkPrimary();
        lock.unlock();

        // make PrePrepare msg
        lock.lock();
        PbftMessage prePrepareMsg = makePrePrepareMsg();
        lock.unlock();
        if (prePrepareMsg != null) {
            multicastMessage(prePrepareMsg);
        } else {
            if (!waitingForMessage("PREPREPA")) {
                failCount++;
                log.debug("PREPREPARE message is not received.");
            }
        }

        // make Prepare msg
        lock.lock();
        PbftMessage prepareMsg = makePrepareMsg();
        lock.unlock();
        if (prepareMsg != null) {
            multicastMessage(prepareMsg);
            if (!waitingForMessage("PREPAREM")) {
                log.debug("PREPAREM messages are not enough.");
            }
        }

        // make commit msg
        lock.lock();
        PbftMessage commitMsg = makeCommitMsg();
        lock.unlock();
        if (commitMsg != null) {
            multicastMessage(commitMsg);
            if (!waitingForMessage("COMMITMS")) {
                log.debug("COMMITMS messages are not enough.");
            }
        }

        lock.lock();
        PbftBlock block = confirmFinalBlock();
        if (block != null) {
            resetUnConfirmedBlock(block.getIndex());
        }
        lock.unlock();
        if (block != null) {
            broadcastBlock(block, this.proxyNodeMap);
        }
    }

    private boolean waitingForMessage(String message) {
        int messageCount;
        for (int i = 0; i < consensusCount; i++) {
            switch (message) {
                case "PREPREPA":
                    messageCount = 1;
                    break;
                default:
                    messageCount = consensusCount;
            }

            if (getMsgMap(blockChain.getLastConfirmedBlock().getIndex() + 1, message).size()
                    < messageCount) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    log.trace(e.getMessage());
                }
            } else {
                return true;
            }
        }
        return false;
    }

    private void loggingStatus() {
        log.trace("loggingStatus");
        log.debug("failCount= " + this.failCount);
        log.debug("isActive=" + this.isActive);
        log.debug("isSynced=" + this.isSynced);
        log.debug("isPrePrepared= " + this.isPrePrepared);
        log.debug("isPrepared= " + this.isPrepared);
        log.debug("isCommitted= " + this.isCommitted);
        log.debug("isViewChanged= " + this.isViewChanged);

        log.debug("unConfirmedMsgMap size= " + this.blockChain.getUnConfirmedData().size());
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

    private void broadcastBlock(PbftBlock block, Map<String, PbftClientStub> clientMap) {
        for (String key : clientMap.keySet()) {
            PbftClientStub client = clientMap.get(key);
            if (client.isMyclient()) {
                continue;
            }
            try {
                client.broadcastPbftBlock(PbftBlock.toProto(block));
                log.debug("BroadcastBlock [{}]{} to {}:{}", block.getIndex(), block.getHashHex(),
                        client.getHost(), client.getPort());
            } catch (Exception e) {
                log.debug("BroadcastBlock exception: " + e.getMessage());
                log.debug("client: " + client.getId());
            }
        }
    }

    private PbftMessage makePrePrepareMsg() {
        if (!this.isPrimary
                || this.isPrePrepared) {
            return null;
        }

        byte[] prevBlockHash = this.blockChain.getLastConfirmedBlock().getHash();

        Block newBlock = makeNewBlock(seqNumber, prevBlockHash);
        log.trace("newBlock" + newBlock.toString());

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

        this.blockChain.getUnConfirmedData().put(prePrepare.getSignatureHex(), prePrepare);
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
        Map<String, PbftMessage> viewChangeMsgMap = getMsgMap(seqNumber, "VIEWCHAN");

        if (viewChangeMsgMap.size() >= consensusCount) {

            for (int i = 0; i < viewChangeMsgMap.size(); i++) {

                long count = 0;
                for (int j = 0; j < viewChangeMsgMap.size(); j++) {
                    if (((PbftMessage) viewChangeMsgMap.values().toArray()[i]).getViewNumber()
                            == ((PbftMessage) viewChangeMsgMap.values().toArray()[j])
                            .getViewNumber()) {
                        count++;
                    }
                }
                if (count >= consensusCount) {
                    return ((PbftMessage) viewChangeMsgMap.values().toArray()[i]).getViewNumber();
                }
            }
        }

        return seqNumber;
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
        List<TransactionHusk> txHusks = new ArrayList<>(blockChain.getTransactionStore()
                .getUnconfirmedTxs());

        for (TransactionHusk txHusk : txHusks) {
            txs.add(txHusk.getCoreTransaction());
        }

        BlockBody newBlockBody = new BlockBody(txs);
        txs.clear();
        txHusks.clear();

        BlockHeader newBlockHeader = new BlockHeader(
                blockChain.getChain(),
                Constants.EMPTY_BYTE8,
                Constants.EMPTY_BYTE8,
                prevBlockHash,
                index,
                TimeUtils.time(),
                newBlockBody);
        return new Block(newBlockHeader, wallet, newBlockBody);
    }

    private PbftMessage makePrepareMsg() {
        if (!this.isPrePrepared
                || this.isPrepared) {
            return null;
        }

        // todo : check 1 more PREPREPARE msg
        PbftMessage prePrepareMsg = (PbftMessage) getMsgMap(seqNumber, "PREPREPA").values()
                .toArray()[0];
        if (prePrepareMsg == null) {
            return null;
        }

        PbftMessage prepareMsg = new PbftMessage(
                "PREPAREM",
                viewNumber,
                seqNumber,
                prePrepareMsg.getHash(),
                null,
                wallet,
                null);
        if (prepareMsg.getSignature() == null) {
            prepareMsg.clear();
            return null;
        }

        this.blockChain.getUnConfirmedData().put(prepareMsg.getSignatureHex(), prepareMsg);
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
        if (!this.isPrepared
                || this.isCommitted) {
            return null;
        }

        Map<String, PbftMessage> prepareMsgMap = getMsgMap(seqNumber, "PREPAREM");
        if (prepareMsgMap == null) {
            return null;
        } else if (prepareMsgMap.size() < consensusCount) {
            prepareMsgMap.clear();
            return null;
        }

        PbftMessage commitMsg = new PbftMessage("COMMITMS", viewNumber, seqNumber,
                ((PbftMessage) prepareMsgMap.values().toArray()[0]).getHash(), null, wallet, null);
        if (commitMsg.getSignature() == null) {
            prepareMsgMap.clear();
            commitMsg.clear();
            return null;
        }

        this.blockChain.getUnConfirmedData().put(commitMsg.getSignatureHex(), commitMsg);
        this.isCommitted = true;

        log.debug("make CommitMsg "
                + "("
                + commitMsg.getViewNumber()
                + ")"
                + " ["
                + commitMsg.getSeqNumber()
                + "] "
                + commitMsg.getHashHex());

        prepareMsgMap.clear();

        return commitMsg;
    }

    private PbftBlock confirmFinalBlock() {
        if (!isCommitted) {
            return null;
        }

        long index = this.blockChain.getLastConfirmedBlock().getIndex() + 1;
        PbftMessage prePrepareMsg = null;
        Map<String, PbftMessage> prepareMessageMap = new TreeMap<>();
        Map<String, PbftMessage> commitMessageMap = new TreeMap<>();
        Map<String, PbftMessage> viewChangeMessageMap = new TreeMap<>();

        for (String key : this.blockChain.getUnConfirmedData().keySet()) {
            PbftMessage pbftMessage = this.blockChain.getUnConfirmedData().get(key);
            if (pbftMessage == null) {
                this.blockChain.getUnConfirmedData().remove(key);
            } else if (pbftMessage.getSeqNumber() < index) {
                pbftMessage.clear();
                this.blockChain.getUnConfirmedData().remove(key);
            } else if (pbftMessage.getSeqNumber() == index) {
                switch (pbftMessage.getType()) {
                    case "PREPREPA":
                        if (prePrepareMsg != null) {
                            // todo: for debugging log
                            log.warn("PrePrepare msg is duplicated.");
                            pbftMessage.clear();
                            this.blockChain.getUnConfirmedData().remove(key);
                        } else {
                            prePrepareMsg = pbftMessage;
                        }
                        break;
                    case "PREPAREM":
                        prepareMessageMap.put(key, pbftMessage);
                        break;
                    case "COMMITMS":
                        commitMessageMap.put(key, pbftMessage);
                        break;
                    case "VIEWCHAN":
                        viewChangeMessageMap.put(key, pbftMessage);
                        break;
                    default:
                        log.warn("Invalid message type :" + pbftMessage.getType());
                        break;
                }
            }
        }

        if (prePrepareMsg == null) {
            for (PbftMessage pbftMessage : prepareMessageMap.values()) {
                if (pbftMessage != null) {
                    pbftMessage.clear();
                }
            }
            prepareMessageMap.clear();

            for (PbftMessage pbftMessage : commitMessageMap.values()) {
                if (pbftMessage != null) {
                    pbftMessage.clear();
                }
            }
            commitMessageMap.clear();

            for (PbftMessage pbftMessage : viewChangeMessageMap.values()) {
                if (pbftMessage != null) {
                    pbftMessage.clear();
                }
            }
            viewChangeMessageMap.clear();
        } else if (prepareMessageMap.size() >= consensusCount
                && commitMessageMap.size() >= consensusCount) {
            PbftMessageSet pbftMessageSet = new PbftMessageSet(
                    prePrepareMsg, prepareMessageMap, commitMessageMap, viewChangeMessageMap);
            PbftBlock pbftBlock = new PbftBlock(prePrepareMsg.getBlock(), pbftMessageSet);
            confirmedBlock(pbftBlock);
            return pbftBlock.clone();
        }

        return null;
    }

    private PbftMessage makeViewChangeMsg() {
        if (this.failCount < FAIL_COUNT
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
            viewChangeMsg.clear();
            return null;
        }

        this.blockChain.getUnConfirmedData().put(viewChangeMsg.getSignatureHex(), viewChangeMsg);
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

    private void confirmedBlock(PbftBlock block) {
        this.blockChain.addBlock(block);
    }

    private void resetUnConfirmedBlock(long index) {
        for (String key : this.blockChain.getUnConfirmedData().keySet()) {
            PbftMessage pbftMessage = this.blockChain.getUnConfirmedData().get(key);
            if (pbftMessage.getSeqNumber() <= index) {
                pbftMessage.clear();
                this.blockChain.getUnConfirmedData().remove(key);
            }
        }

        this.isPrePrepared = false;
        this.isPrepared = false;
        this.isCommitted = false;
        this.failCount = 0;
        this.isViewChanged = false;

        this.viewNumber = index + 1;
        this.seqNumber = index + 1;
    }

    private Map<String, PbftMessage> getMsgMap(long index, String msg) {
        Map<String, PbftMessage> msgMap = new TreeMap<>();
        for (String key : this.blockChain.getUnConfirmedData().keySet()) {
            PbftMessage pbftMessage = this.blockChain.getUnConfirmedData().get(key);
            if (pbftMessage.getSeqNumber() == index
                    && pbftMessage.getType().equals(msg)) {
                msgMap.put(key, pbftMessage);
            }
        }
        return msgMap;
    }

    private void checkPrimary() {
        this.viewNumber = getCurrentViewNumber(this.seqNumber);
        int primaryIndex = (int) (this.viewNumber % totalValidatorMap.size());
        currentPrimaryAddr = (String) totalValidatorMap.keySet().toArray()[primaryIndex];

        log.debug("viewNumber: " + this.viewNumber);
        log.debug("seqNumber: " + this.seqNumber);
        log.debug("Primary Index: " + primaryIndex);
        log.debug("currentPrimaryAddr: " + currentPrimaryAddr);

        this.isPrimary = currentPrimaryAddr.equals(this.myNode.getAddr());
    }

    private Map<String, PbftMessage> getViewChangeMsgMap(long index) {
        Map<String, PbftMessage> viewChangeMsgMap = new TreeMap<>();
        for (String key : this.blockChain.getUnConfirmedData().keySet()) {
            PbftMessage pbftMessage = this.blockChain.getUnConfirmedData().get(key);
            if (pbftMessage.getSeqNumber() == index
                    && pbftMessage.getType().equals("VIEWCHAN")) {
                viewChangeMsgMap.put(key, pbftMessage);
            }
        }
        return viewChangeMsgMap;
    }

    public void checkNode() {
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
        pbftStatus.clear();
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
                blockSyncing(client.getAddr(), pbftStatus.getIndex());
            } else if (pbftStatus.getIndex()
                    == this.blockChain.getLastConfirmedBlock().getIndex()) {
                // update unConfirm pbftMessage
                updateUnconfirmedMsgMap(pbftStatus.getUnConfirmedPbftMessageMap());
            }
        } else {
            client.setIsRunning(false);
        }
    }

    private void blockSyncing(String addr, long index) {
        PbftClientStub client = totalValidatorMap.get(addr);
        PbftBlock pbftBlock;
        if (client.isRunning()) {
            List<PbftBlock> pbftBlockList = client.getBlockList(
                    this.blockChain.getLastConfirmedBlock().getIndex());

            log.debug("node: " + client.getId());
            log.debug("index: " + (pbftBlockList != null ? pbftBlockList.get(0).getIndex() : null));
            log.debug("blockList size: " + (pbftBlockList != null ? pbftBlockList.size() : null));

            if (pbftBlockList == null) {
                return;
            } else if (pbftBlockList.size() == 0) {
                pbftBlockList.clear();
                return;
            }

            int i = 0;
            for (; i < pbftBlockList.size(); i++) {
                pbftBlock = pbftBlockList.get(i);
                if (!PbftBlock.verify(pbftBlock)) {
                    log.warn("Verify Fail");
                    for (PbftBlock pbBlock : pbftBlockList) {
                        pbBlock.clear();
                    }
                    pbftBlockList.clear();
                    return;
                }
                this.blockChain.addBlock(pbftBlock);
            }
            pbftBlock = pbftBlockList.get(i - 1);
            resetUnConfirmedBlock(pbftBlock.getIndex());

            for (PbftBlock pbBlock : pbftBlockList) {
                pbBlock.clear();
            }
            pbftBlockList.clear();

        }

        if (this.blockChain.getLastConfirmedBlock().getIndex() < index) {
            blockSyncing(addr, index);
        }
    }

    public void updateUnconfirmedMsg(PbftMessage newPbftMessage) {
        if (!this.blockChain.getUnConfirmedData().containsKey(newPbftMessage.getSignatureHex())) {
            this.blockChain.getUnConfirmedData()
                    .put(newPbftMessage.getSignatureHex(), newPbftMessage.clone());
        }

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
        for (String key : this.blockChain.getUnConfirmedData().keySet()) {
            PbftMessage pbftMessage = this.blockChain.getUnConfirmedData().get(key);
            if (pbftMessage != null && pbftMessage.getSeqNumber() == index + 1) {
                pbftMessageMap.put(key, pbftMessage.clone());
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

    @SuppressWarnings("unchecked")
    private TreeMap<String, PbftClientStub> initTotalValidator() {
        TreeMap<String, PbftClientStub> nodeMap = new TreeMap<>();
        try {
            Map<String, Object> validatorInfoMap =
                    this.defaultConfig.getConfig().getConfig("yggdrash.validator.info").root().unwrapped();
            for (String key : validatorInfoMap.keySet()) {
                String host = ((Map<String, String>) validatorInfoMap.get(key)).get("host");
                int port = ((Map<String, Integer>) validatorInfoMap.get(key)).get("port");
                PbftClientStub client = new PbftClientStub(key, host, port);
                if (client.getId().equals(myNode.getId())) {
                    nodeMap.put(myNode.getAddr(), myNode);
                } else {
                    nodeMap.put(client.getAddr(), client);
                }
            }
            log.debug("ValidatorInfo: " + nodeMap.toString());
        } catch (ConfigException ce) {
            log.error("Validators is not set.");
            throw new NotValidateException();
        }
        return nodeMap;
    }

    @SuppressWarnings("unchecked")
    private TreeMap<String, PbftClientStub> initProxyNode() {
        TreeMap<String, PbftClientStub> nodeMap = new TreeMap<>();
        try {
            Map<String, Object> proxyNodeMap =
                    this.defaultConfig.getConfig().getConfig("yggdrash.validator.proxyNode").root().unwrapped();
            for (String key : proxyNodeMap.keySet()) {
                String host = ((Map<String, String>) proxyNodeMap.get(key)).get("host");
                int port = ((Map<String, Integer>) proxyNodeMap.get(key)).get("port");
                PbftClientStub client = new PbftClientStub(key, host, port);
                if (client.getId().equals(myNode.getId())) {
                    nodeMap.put(myNode.getAddr(), myNode);
                } else {
                    nodeMap.put(client.getAddr(), client);
                }
            }
            log.debug("ProxyNode: " + nodeMap.toString());
        } catch (ConfigException ce) {
            log.warn("ProxyNode is not set.");
        }
        return nodeMap;
    }

    private PbftClientStub initMyNode() {
        PbftClientStub client = new PbftClientStub(
                wallet.getHexAddress(), this.grpcHost, this.grpcPort);
        client.setMyclient(true);
        client.setIsRunning(true);
        return client;
    }

    private boolean initValidator() {
        log.debug("MyNode ID: " + this.myNode.getId());
        for (PbftClientStub clientStub : totalValidatorMap.values()) {
            if (this.myNode.getId().equals(clientStub.getId())) {
                return true;
            }
        }
        return false;
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
