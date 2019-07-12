package io.yggdrash.validator.service.pbft;

import com.typesafe.config.ConfigException;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.contract.vo.dpoa.Validator;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockBody;
import io.yggdrash.core.blockchain.BlockHeader;
import io.yggdrash.core.blockchain.BlockImpl;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.consensus.ConsensusBlockChain;
import io.yggdrash.core.consensus.ConsensusService;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.validator.data.pbft.PbftBlock;
import io.yggdrash.validator.data.pbft.PbftMessage;
import io.yggdrash.validator.data.pbft.PbftMessageSet;
import io.yggdrash.validator.data.pbft.PbftStatus;
import io.yggdrash.validator.data.pbft.PbftVerifier;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

public class PbftService implements ConsensusService<PbftProto.PbftBlock, PbftMessage> {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(PbftService.class);

    private static final int FAIL_COUNT = 3;

    private final DefaultConfig defaultConfig;
    private final Wallet wallet;
    private final ConsensusBlockChain<PbftProto.PbftBlock, PbftMessage> blockChain;

    private final PbftClientStub myNode;
    private final Map<String, Peer> validatorConfigMap;
    private Map<String, PbftClientStub> totalValidatorMap;
    private final Map<String, PbftClientStub> proxyNodeMap;

    private final ReentrantLock lock = new ReentrantLock();

    private int bftCount;
    private int consensusCount;

    private boolean isActive;
    private boolean isSynced;
    private boolean isPrePrepared;
    private boolean isPrepared;
    private boolean isCommitted;

    private boolean isPrimary;
    private long viewNumber;
    private long seqNumber;
    private String currentPrimaryAddr;

    private int failCount;

    private final String grpcHost;
    private final int grpcPort;

    public PbftService(Wallet wallet,
                       ConsensusBlockChain<PbftProto.PbftBlock, PbftMessage> blockChain,
                       DefaultConfig defaultConfig,
                       String grpcHost,
                       int grpcPort) {
        this.wallet = wallet;
        this.blockChain = blockChain;
        this.defaultConfig = defaultConfig;
        this.grpcHost = grpcHost;
        this.grpcPort = grpcPort;

        this.myNode = initMyNode();
        this.validatorConfigMap = initValidatorConfigMap();
        this.totalValidatorMap = initTotalValidator();
        this.proxyNodeMap = initProxyNode();
        this.bftCount = (totalValidatorMap.size() - 1) / 3;
        this.consensusCount = bftCount * 2 + 1;

        this.isActive = false;
        this.isSynced = false;
        this.isPrePrepared = false;
        this.isPrepared = false;
        this.isCommitted = false;
        this.failCount = 0;

        this.viewNumber = this.blockChain.getBlockChainManager().getLastIndex() + 1;
        this.seqNumber = this.blockChain.getBlockChainManager().getLastIndex() + 1;


        printInitInfo();
    }

    @Override
    public void run() {
        mainScheduler();
    }

    private void mainScheduler() {

        updateTotalValidatorMap();

        if (!isValidator()) {
            log.debug("Node is not validator.");
            return;
        }

        loggingStatus();

        checkNode();

        if (!isActive) {
            log.info("Validators are not activated. {}/{}", getActiveNodeCount(), consensusCount);
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

            if (getMsgMap(this.viewNumber, this.seqNumber, message).size()
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
        log.info("Status: activeNode={}, "
                        + "failCount={}, isActive={}, isSynced={}, isPrePrepared={}, isPrepared={}, isCommitted={} "
                        + "unConfirmedMsgCount={}, unConfirmedTxCount={}",
                getActiveNodeCount(),
                this.failCount, this.isActive, this.isSynced, this.isPrePrepared, this.isPrepared, this.isCommitted,
                this.blockChain.getUnConfirmedData().size(),
                blockChain.getBlockChainManager().getUnconfirmedTxs().size());
        if (log.isTraceEnabled()) {
            for (PbftMessage message : this.blockChain.getUnConfirmedData().values()) {
                log.trace(message.toJsonObject().toString());
            }
        }
        log.debug("");
    }

    private void multicastMessage(PbftMessage message) {
        for (Map.Entry<String, PbftClientStub> entry : totalValidatorMap.entrySet()) {
            PbftClientStub client = entry.getValue();
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
        for (Map.Entry<String, PbftClientStub> entry : clientMap.entrySet()) {
            PbftClientStub client = entry.getValue();
            if (client.isMyclient()) {
                continue;
            }
            try {
                client.broadcastPbftBlock(block.getInstance());
                log.debug("BroadcastBlock [{}]{} to {}:{}", block.getIndex(), block.getHash(),
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

        byte[] prevBlockHash = this.blockChain.getBlockChainManager().getLastHash().getBytes();

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
                + newBlock.getHash()
                + " ("
                + newBlock.getAddress()
                + ")");

        return prePrepare;
    }

    private long getCurrentViewNumber() {
        long newViewNumber = this.viewNumber + 1;
        Map<String, PbftMessage> viewChangeMsgMap = getMsgMap(newViewNumber, this.seqNumber, "VIEWCHAN");

        for (int i = 0; i < viewChangeMsgMap.size(); i++) {
            PbftMessage firstMsg = (PbftMessage) viewChangeMsgMap.values().toArray()[i];
            for (int j = i + 1; j < viewChangeMsgMap.size(); j++) {
                PbftMessage secondMsg = (PbftMessage) viewChangeMsgMap.values().toArray()[j];
                if (firstMsg.getAddress().equals(secondMsg.getAddress())) {
                    log.debug("Messages are duplicated by {}", secondMsg.getAddress());
                    log.debug("First Message {}", firstMsg.toJsonObject().toString());
                    log.debug("Second Message {}", secondMsg.toJsonObject().toString());
                    viewChangeMsgMap.remove(secondMsg.getSignatureHex());
                }
            }
        }

        if (viewChangeMsgMap.size() < consensusCount) {
            return this.viewNumber;
        }
        return newViewNumber;
    }

    private Block makeNewBlock(long index, byte[] prevBlockHash) {
        List<Transaction> txList = new ArrayList<>(blockChain.getBlockChainManager().getUnconfirmedTxs());

        BlockBody newBlockBody = new BlockBody(txList);

        BlockHeader newBlockHeader = new BlockHeader(
                blockChain.getBranchId().getBytes(),
                Constants.EMPTY_BYTE8,
                Constants.EMPTY_BYTE8,
                prevBlockHash,
                index,
                TimeUtils.time(),
                newBlockBody);
        return new BlockImpl(newBlockHeader, wallet, newBlockBody);
    }

    private PbftMessage makePrepareMsg() {
        if (!this.isPrePrepared
                || this.isPrepared) {
            return null;
        }

        // todo : check 1 more PREPREPARE msg
        PbftMessage prePrepareMsg;
        try {
            prePrepareMsg = (PbftMessage) getMsgMap(this.viewNumber, this.seqNumber, "PREPREPA").values()
                    .toArray()[0];
            if (prePrepareMsg == null) {
                return null;
            }
        } catch (Exception e) {
            log.debug(e.getMessage());
            return null;
        }

        PbftMessage prepareMsg = new PbftMessage(
                "PREPAREM",
                viewNumber,
                seqNumber,
                Sha3Hash.createByHashed(prePrepareMsg.getHash()),
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

        Map<String, PbftMessage> prepareMsgMap = getMsgMap(this.viewNumber, this.seqNumber, "PREPAREM");
        if (prepareMsgMap == null) {
            return null;
        } else if (prepareMsgMap.size() < consensusCount) {
            prepareMsgMap.clear();
            return null;
        }
        byte[] hash = ((PbftMessage) prepareMsgMap.values().toArray()[0]).getHash();
        PbftMessage commitMsg = new PbftMessage("COMMITMS", viewNumber, seqNumber,
                Sha3Hash.createByHashed(hash), null, wallet, null);
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

        PbftMessage prePrepareMsg = null;
        Map<String, PbftMessage> prepareMessageMap = new TreeMap<>();
        Map<String, PbftMessage> commitMessageMap = new TreeMap<>();
        Map<String, PbftMessage> viewChangeMessageMap = new TreeMap<>();

        for (String key : this.blockChain.getUnConfirmedData().keySet()) {
            PbftMessage pbftMessage = this.blockChain.getUnConfirmedData().get(key);
            if (pbftMessage == null) {
                this.blockChain.getUnConfirmedData().remove(key);
            } else if (pbftMessage.getSeqNumber() < this.seqNumber
                    || pbftMessage.getViewNumber() < this.viewNumber) {
                pbftMessage.clear();
                this.blockChain.getUnConfirmedData().remove(key);
            } else if (pbftMessage.getSeqNumber() == this.seqNumber
                    || pbftMessage.getViewNumber() == this.viewNumber) {
                switch (pbftMessage.getType()) {
                    case "PREPREPA":
                        if (prePrepareMsg != null) {
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
            return pbftBlock;
        }

        return null;
    }

    private PbftMessage makeViewChangeMsg() {
        if (this.failCount < FAIL_COUNT
                || this.isPrePrepared == true) {
            return null;
        }

        Block block = this.blockChain.getBlockChainManager().getLastConfirmedBlock().getBlock();
        log.trace("block" + block.toString());
        long newViewNumber = this.viewNumber + 1;

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

        log.warn("ViewChanged"
                + " ("
                + seqNumber
                + ") ->"
                + " ("
                + newViewNumber
                + ")");

        return viewChangeMsg;
    }

    public void confirmedBlock(PbftBlock block) {
        this.blockChain.addBlock(block);
        resetUnConfirmedBlock(block.getIndex());
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

        this.viewNumber = (this.viewNumber > index + 1 ? this.viewNumber : index + 1);
        this.seqNumber = index + 1;
    }

    private Map<String, PbftMessage> getMsgMap(long viewNumber, long seqNumber, String msg) {
        Map<String, PbftMessage> msgMap = new TreeMap<>();
        for (String key : this.blockChain.getUnConfirmedData().keySet()) {
            PbftMessage pbftMessage = this.blockChain.getUnConfirmedData().get(key);
            if (pbftMessage.getViewNumber() == viewNumber
                    && pbftMessage.getSeqNumber() == seqNumber
                    && pbftMessage.getType().equals(msg)) {
                msgMap.put(key, pbftMessage);
            }
        }
        return msgMap;
    }

    private void checkPrimary() {
        long checkViewNumber = getCurrentViewNumber();
        if (checkViewNumber > this.viewNumber) {
            this.viewNumber = checkViewNumber;
            resetUnConfirmedMessage(this.viewNumber, this.seqNumber);
        }

        int primaryIndex = (int) (this.viewNumber % totalValidatorMap.size());
        currentPrimaryAddr = (String) totalValidatorMap.keySet().toArray()[primaryIndex];
        log.debug("viewNumber={}, seqNumber={}, primaryIndex={}, primaryAddr={}",
                this.viewNumber, this.seqNumber, primaryIndex, currentPrimaryAddr);

        this.isPrimary = currentPrimaryAddr.equals(this.myNode.getAddr());
    }

    private void resetUnConfirmedMessage(long viewNumber, long seqNumber) {
        for (String key : this.blockChain.getUnConfirmedData().keySet()) {
            PbftMessage pbftMessage = this.blockChain.getUnConfirmedData().get(key);
            if (pbftMessage.getViewNumber() < viewNumber
                    || pbftMessage.getSeqNumber() < seqNumber) {
                pbftMessage.clear();
                this.blockChain.getUnConfirmedData().remove(key);
            }
        }

        this.isPrePrepared = false;
        this.isPrepared = false;
        this.isCommitted = false;
        this.failCount = 0;
    }

    public void checkNode() {
        for (Map.Entry<String, PbftClientStub> entry : totalValidatorMap.entrySet()) {
            PbftClientStub client = entry.getValue();
            if (client.isMyclient() || client.getChannel() == null) {
                continue;
            }

            long pingTime = System.currentTimeMillis();
            long pongTime = client.pingPongTime(pingTime);

            if (pongTime > 0L) {
                checkNodeStatus(client);
            } else {
                log.info("Cannot connect to {} ping {} pong {}", client.toString(), pingTime, pongTime);
                client.setIsRunning(false);
            }
        }

        this.isSynced = true;
        setActiveMode();
    }

    private void checkNodeStatus(PbftClientStub client) {
        PbftStatus myStatus = getMyNodeStatus();
        log.trace("My PbftStatus is {}", myStatus.toJsonObject().toString());

        PbftStatus pbftStatus = client.exchangePbftStatus(PbftStatus.toProto(myStatus));
        if (pbftStatus == null) {
            client.setIsRunning(false);
            return;
        }
        log.trace("Client {} PbftStatus is {}", client.toString(), myStatus.toJsonObject().toString());

        updateStatus(client, pbftStatus);
        pbftStatus.clear();
    }

    private void updateStatus(PbftClientStub client, PbftStatus pbftStatus) {
        if (PbftStatus.verify(pbftStatus)) {
            client.setIsRunning(true);

            long lastConfirmedBlockIndex = this.blockChain.getBlockChainManager().getLastIndex();
            if (pbftStatus.getIndex() > lastConfirmedBlockIndex) {
                log.debug("this Index: " + lastConfirmedBlockIndex);
                log.debug("client Index: " + pbftStatus.getIndex());
                log.debug("client : " + client.getId());

                this.isSynced = false;
                blockSyncing(client.getAddr(), pbftStatus.getIndex());
            } else if (pbftStatus.getIndex() == lastConfirmedBlockIndex) {
                // update unConfirm pbftMessage
                updateUnconfirmedMsgMap(pbftStatus.getUnConfirmedPbftMessageMap());
            }
        } else {
            client.setIsRunning(false);
        }
    }

    private void blockSyncing(String addr, long untilBlockIndex) {
        PbftClientStub client = totalValidatorMap.get(addr);
        if (!client.isRunning()) {
            return;
        }

        PbftBlock pbftBlock;
        long lastConfirmedBlockIndex = this.blockChain.getBlockChainManager().getLastIndex();
        log.debug("Block syncing Node: {} From: {} To: {}",
                client.getId(), lastConfirmedBlockIndex + 1, untilBlockIndex);

        List<PbftBlock> pbftBlockList = client.getBlockList(lastConfirmedBlockIndex + 1);
        log.debug("BlockList size: {}", (pbftBlockList != null ? pbftBlockList.size() : null));

        if (pbftBlockList == null) {
            return;
        } else if (pbftBlockList.size() == 0) {
            pbftBlockList.clear();
            return;
        }

        int i = 0;
        for (; i < pbftBlockList.size(); i++) {
            pbftBlock = pbftBlockList.get(i);
            if (!PbftVerifier.INSTANCE.verify(pbftBlock)
                    || this.blockChain.addBlock(pbftBlock) == null) {
                log.warn("Failed verifing a block when syncing");
                client.setIsRunning(false);
                for (PbftBlock pbBlock : pbftBlockList) {
                    pbBlock.clear();
                }
                pbftBlockList.clear();
                return;
            }
        }

        pbftBlock = pbftBlockList.get(i - 1);
        resetUnConfirmedBlock(pbftBlock.getIndex());

        for (PbftBlock pbBlock : pbftBlockList) {
            pbBlock.clear();
        }
        pbftBlockList.clear();

        if (this.blockChain.getBlockChainManager().getLastIndex() < untilBlockIndex) {
            blockSyncing(addr, untilBlockIndex);
        }
    }

    public void updateUnconfirmedMsg(PbftMessage newPbftMessage) {
        if (!this.blockChain.getUnConfirmedData().containsKey(newPbftMessage.getSignatureHex())) {
            this.blockChain.getUnConfirmedData()
                    .put(newPbftMessage.getSignatureHex(), newPbftMessage.clone());
        }

        if (newPbftMessage.getType().equals("PREPREPA")
                && newPbftMessage.getSeqNumber() == this.seqNumber
                && newPbftMessage.getViewNumber() == this.viewNumber) {
            this.isPrePrepared = true;
        }
    }

    public void updateUnconfirmedMsgMap(Map<String, PbftMessage> newPbftMessageMap) {
        for (Map.Entry<String, PbftMessage> entry : newPbftMessageMap.entrySet()) {
            PbftMessage pbftMessage = entry.getValue();

            if (pbftMessage.getViewNumber() >= this.viewNumber
                    || pbftMessage.getSeqNumber() >= this.seqNumber) {
                updateUnconfirmedMsg(entry.getValue());
            }
        }
    }

    public PbftStatus getMyNodeStatus() {
        long index = this.blockChain.getBlockChainManager().getLastIndex();
        Map<String, PbftMessage> pbftMessageMap = new TreeMap<>();
        for (String key : this.blockChain.getUnConfirmedData().keySet()) {
            PbftMessage pbftMessage = this.blockChain.getUnConfirmedData().get(key);
            if (pbftMessage != null
                    && pbftMessage.getSeqNumber() > index
                    && pbftMessage.getViewNumber() >= this.viewNumber) {
                pbftMessageMap.put(key, pbftMessage.clone());
            }
        }
        long timestamp = TimeUtils.time();
        return new PbftStatus(index, pbftMessageMap, timestamp, wallet);
    }

    private void printInitInfo() {
        log.info("Node Started");
        log.info("wallet address: {}", wallet.getHexAddress());
        log.info("wallet pubKey: {}", Hex.toHexString(wallet.getPubicKey()));
        log.info("validatorList: {}", this.totalValidatorMap);
        if (!isValidator()) {
            log.error("This node is not validator. {}", myNode.toString());
        }
    }

    private Map<String, Peer> initValidatorConfigMap() {
        Map<String, Peer> peerMap = new TreeMap<>();

        Map<String, Object> validatorInfoMap =
                this.defaultConfig.getConfig().getConfig("yggdrash.validator.info").root().unwrapped();
        for (Map.Entry<String, Object> entry : validatorInfoMap.entrySet()) {
            String host = ((Map<String, String>) entry.getValue()).get("host");
            int port = ((Map<String, Integer>) entry.getValue()).get("port");
            peerMap.put(entry.getKey(), Peer.valueOf(entry.getKey(), host, port));
        }
        return peerMap;
    }

    private TreeMap<String, PbftClientStub> initTotalValidator() {
        TreeMap<String, PbftClientStub> nodeMap = new TreeMap<>();
        try {
            for (Validator validator : blockChain.getValidators().getValidatorMap().values()) {
                String address = validator.getAddr();
                Peer peer = validatorConfigMap.get(address);
                updateNodeMap(nodeMap, address,
                        peer != null ? peer.getHost() : "",
                        peer != null ? peer.getPort() : 0);
            }
            log.trace("ValidatorInfo: {}", nodeMap);
        } catch (Exception e) {
            log.error("Validator configuration is not valid");
            throw new NotValidateException("Validator configuration is not valid");
        }
        return nodeMap;
    }

    // TODO: test more cases using DPoA contract
    // TODO: consider excuting when adding the new block
    private void updateTotalValidatorMap() {
        if (totalValidatorMap.keySet().equals(blockChain.getValidators().getValidatorMap().keySet())) {
            return;
        }

        log.trace("Current Validators: {}", totalValidatorMap.keySet());
        log.trace("New Validators: {}", blockChain.getValidators().getValidatorMap().keySet());

        this.totalValidatorMap = initTotalValidator();

        this.bftCount = (totalValidatorMap.size() - 1) / 3;
        this.consensusCount = bftCount * 2 + 1;
    }

    @SuppressWarnings("unchecked")
    private TreeMap<String, PbftClientStub> initProxyNode() {
        TreeMap<String, PbftClientStub> nodeMap = new TreeMap<>();
        try {
            Map<String, Object> proxyNodeMap =
                    this.defaultConfig.getConfig().getConfig("yggdrash.validator.proxyNode").root().unwrapped();
            for (Map.Entry<String, Object> entry : proxyNodeMap.entrySet()) {
                String host = ((Map<String, String>) entry.getValue()).get("host");
                int port = ((Map<String, Integer>) entry.getValue()).get("port");
                updateNodeMap(nodeMap, entry.getKey(), host, port);
            }
            log.debug("ProxyNode: {}", nodeMap.toString());
        } catch (ConfigException ce) {
            log.warn("ProxyNode is not set.");
        }
        return nodeMap;
    }

    private void updateNodeMap(Map<String, PbftClientStub> nodeMap, String address, String host, int port) {
        PbftClientStub client = new PbftClientStub(address, host, port);
        if (client.getId().equals(myNode.getId())) {
            nodeMap.put(myNode.getAddr(), myNode);
        } else {
            nodeMap.put(client.getAddr(), client);
        }
    }

    private PbftClientStub initMyNode() {
        PbftClientStub client = new PbftClientStub(wallet.getHexAddress(), this.grpcHost, this.grpcPort);
        client.setMyclient(true);
        client.setIsRunning(true);
        log.debug("MyNode ID: {}", client.getId());
        return client;
    }

    private boolean isValidator() {
        return totalValidatorMap.containsKey(myNode.getAddr());
    }

    private void setActiveMode() {
        int runningNodeCount = getActiveNodeCount();
        if (runningNodeCount >= consensusCount) {
            if (!this.isActive) {
                this.isActive = true;
                log.info("ValidatorNode is activated.");
            }
        } else {
            if (this.isActive) {
                this.isActive = false;
                log.info("ValidatorNode is deactivated.");
            }
        }
    }

    private int getActiveNodeCount() {
        int count = 0;
        for (Map.Entry<String, PbftClientStub> entry : totalValidatorMap.entrySet()) {
            if (entry.getValue().isRunning()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public ReentrantLock getLock() {
        return lock;
    }

    @Override
    public ConsensusBlockChain<PbftProto.PbftBlock, PbftMessage> getBlockChain() {
        return blockChain;
    }

}
