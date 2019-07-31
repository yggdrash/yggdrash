package io.yggdrash.validator.service.ebft;

import com.google.protobuf.ByteString;
import com.typesafe.config.ConfigException;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockBody;
import io.yggdrash.core.blockchain.BlockHeader;
import io.yggdrash.core.blockchain.BlockImpl;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.consensus.ConsensusBlockChain;
import io.yggdrash.core.consensus.ConsensusService;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.EbftProto;
import io.yggdrash.validator.data.ebft.EbftBlock;
import io.yggdrash.validator.data.ebft.EbftStatus;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

public class EbftService implements ConsensusService<EbftProto.EbftBlock, EbftBlock, EbftClientStub> {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(EbftService.class);

    private final boolean isValidator;
    private final int consensusCount;

    private final Wallet wallet;
    private final ConsensusBlockChain<EbftProto.EbftBlock, EbftBlock> blockChain;
    private final DefaultConfig defaultConfig;

    private final EbftClientStub myNode;
    private final Map<String, EbftClientStub> totalValidatorMap;
    private final Map<String, EbftClientStub> proxyNodeMap;

    private final ReentrantLock lock = new ReentrantLock();

    private boolean isActive;
    private boolean isSynced;
    private boolean isProposed;
    private boolean isConsensused;

    private final String grpcHost;
    private final int grpcPort;

    public EbftService(Wallet wallet,
                       ConsensusBlockChain<EbftProto.EbftBlock, EbftBlock> blockChain,
                       DefaultConfig defaultConfig,
                       String grpcHost,
                       int grpcPort) {
        this.wallet = wallet;
        this.blockChain = blockChain;
        this.defaultConfig = defaultConfig;
        this.grpcHost = grpcHost;
        this.grpcPort = grpcPort;

        this.myNode = initMyNode();
        this.totalValidatorMap = initTotalValidator();
        this.proxyNodeMap = initProxyNode();
        this.isValidator = initValidator();
        this.isActive = false;
        this.isSynced = false;
        if (totalValidatorMap.size() != 0) {
            this.consensusCount = totalValidatorMap.size() / 2 + 1;
        } else {
            this.consensusCount = 0;
            throw new NotValidateException();
        }

        printInitInfo();
    }

    @Override
    public void run() {
        mainScheduler();
    }

    private void mainScheduler() {
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
        EbftBlock proposedEbftBlock = makeProposedBlock();
        lock.unlock();
        if (proposedEbftBlock != null) {
            multicastBlock(proposedEbftBlock);
            if (!waitingProposedBlock()) {
                log.debug("ProposedBlock count is not enough.");
            }
        }

        lock.lock();
        EbftBlock consensusedEbftBlock = makeConsensus();
        lock.unlock();
        if (consensusedEbftBlock != null) {
            multicastBlock(consensusedEbftBlock);
            if (!waitingConsensusedBlock()) {
                log.debug("ConsensusedBlock count is not enough.");
            }
        }

        lock.lock();
        EbftBlock block = confirmFinalBlock();
        if (block != null) {
            resetUnConfirmedBlock(block.getIndex());
        }
        lock.unlock();
        if (block != null) {
            broadcastBlock(block, this.proxyNodeMap);
        }
    }

    private boolean waitingProposedBlock() {
        for (int i = 0; i < consensusCount; i++) {
            if (getUnconfirmedEbftBlockCount(blockChain.getUnConfirmedData(),
                    blockChain.getBlockChainManager().getLastIndex() + 1)
                    < consensusCount) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    log.trace(e.getMessage());
                    Thread.currentThread().interrupt();
                }
            } else {
                return true;
            }
        }
        return false;
    }

    private boolean waitingConsensusedBlock() {
        for (int i = 0; i < consensusCount; i++) {
            for (EbftBlock unConfirmedEbftBlock : blockChain.getUnConfirmedData().values()) {
                if (unConfirmedEbftBlock.getIndex()
                        == blockChain.getBlockChainManager().getLastIndex() + 1
                        && unConfirmedEbftBlock.getConsensusMessages().size() > consensusCount) {
                    return true;
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.trace(e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        return false;
    }

    private void checkNode() {
        for (EbftClientStub client : totalValidatorMap.values()) {
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

    private void checkNodeStatus(EbftClientStub client) {
        EbftStatus ebftStatus = client.exchangeNodeStatus(EbftStatus.toProto(getMyNodeStatus()));
        updateStatus(client, ebftStatus);
    }

    private void updateStatus(EbftClientStub client, EbftStatus ebftStatus) {
        if (!EbftStatus.verify(ebftStatus)) {
            client.setIsRunning(false);
            return;
        }

        client.setIsRunning(true);

        long lastConfirmedBlock = this.blockChain.getBlockChainManager().getLastIndex();

        if (ebftStatus.getIndex() > lastConfirmedBlock) {
            log.debug("this Index: {}", lastConfirmedBlock);
            log.debug("client Index: {}", ebftStatus.getIndex());
            log.debug("client : {}", client.getId());

            this.isSynced = false;
            blockSyncing(client.getAddr(), ebftStatus.getIndex());
        } else if (ebftStatus.getIndex() == lastConfirmedBlock) {
            for (EbftBlock ebftBlock : ebftStatus.getUnConfirmedEbftBlockList()) {
                updateUnconfirmedBlock(ebftBlock);
            }
        }
    }

    private void blockSyncing(String addr, long index) {
        EbftClientStub client = totalValidatorMap.get(addr);
        log.debug("node: {}", client.getId());
        log.debug("index: {}", index);
        if (!client.isRunning()) {
            return;
        }

        long lastConfirmedBlockIndex = this.blockChain.getBlockChainManager().getLastIndex();
        List<EbftBlock> ebftBlockList = new ArrayList<>(client.getBlockList(lastConfirmedBlockIndex));

        if (ebftBlockList.isEmpty()) {
            return;
        }

        log.debug("node: {}", client.getId());
        log.debug("index: {}", (ebftBlockList.get(0) != null ? ebftBlockList.get(0).getIndex() : null));
        log.debug("blockList size: {}", ebftBlockList.size());

        EbftBlock ebftBlock;
        int i = 0;
        for (; i < ebftBlockList.size(); i++) {
            ebftBlock = ebftBlockList.get(i);
            if (!EbftBlock.verify(ebftBlock) || !consensusVerify(ebftBlock)) {
                log.warn("Verify Fail");
                for (EbftBlock ebBlock : ebftBlockList) {
                    ebBlock.clear();
                }
                ebftBlockList.clear();
                return;
            }
            this.blockChain.addBlock(ebftBlock);
        }

        ebftBlock = ebftBlockList.get(i - 1);

        //todo: if consensusCount(validator count) is different from previous count,
        // cannot confirm prevBlock.
        if (ebftBlock.getConsensusMessages().size() >= consensusCount) {
            resetUnConfirmedBlock(ebftBlock.getIndex());
            this.isProposed = false;
            this.isConsensused = false;
        }

        ebftBlockList.clear();

        if (lastConfirmedBlockIndex < index) {
            blockSyncing(addr, index);
        }
    }

    private EbftBlock makeProposedBlock() {
        if (this.isProposed
                || !this.isSynced) {
            return null;
        }

        long index = this.blockChain.getBlockChainManager().getLastIndex() + 1;
        byte[] prevBlockHash = this.blockChain.getBlockChainManager().getLastHash().getBytes();

        Block newBlock = makeNewBlock(index, prevBlockHash);
        log.trace("newBlock{}", newBlock);

        EbftBlock newEbftBlock = new EbftBlock(newBlock);

        // add in unconfirmed blockMap & unconfirmed block
        this.blockChain.getUnConfirmedData()
                .putIfAbsent(newEbftBlock.getHash().toString(), newEbftBlock);
        this.isProposed = true;

        log.debug("make Proposed Block"
                + "["
                + newEbftBlock.getIndex()
                + "]"
                + newEbftBlock.getHash()
                + " ("
                + newEbftBlock.getBlock().getAddress()
                + ")");

        return newEbftBlock;
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

    private EbftBlock makeConsensus() {
        if (this.isConsensused || !this.isSynced) {
            return null;
        }

        Map<String, EbftBlock> unConfirmedEbftBlockMap =
                this.blockChain.getUnConfirmedData();
        long lastConfirmedBlockIndex = this.blockChain.getBlockChainManager().getLastIndex();
        int unconfirmedEbftBlockCount = getUnconfirmedEbftBlockCount(
                unConfirmedEbftBlockMap, lastConfirmedBlockIndex + 1);

        //todo : check active count, active status
        if (unconfirmedEbftBlockCount < getActiveNodeCount()
                || unconfirmedEbftBlockCount < consensusCount
                || !checkReceiveProposedEbftBlock()) {
            log.debug("Cannot makeConsensus: unConfirmedBlock: {} ActiveNode: {}  proposedEbftBlock: {}",
                    unconfirmedEbftBlockCount, getActiveNodeCount(), checkReceiveProposedEbftBlock());
            return null;
        }

        //todo: check efficiency
        String minKey = null;
        for (Map.Entry<String, EbftBlock> entry : unConfirmedEbftBlockMap.entrySet()) {
            if (entry.getValue().getIndex() != lastConfirmedBlockIndex + 1) {
                continue;
            }
            if (minKey == null) {
                minKey = entry.getKey();
            } else {
                if (Arrays.compareUnsigned(Hex.decode(minKey),
                        Hex.decode(entry.getKey())) > 0) {
                    minKey = entry.getKey();
                }
            }
        }

        EbftBlock ebftBlock = unConfirmedEbftBlockMap.get(minKey);
        ByteString consensus = wallet.signByteString(ebftBlock.getHash().getBytes(), true);
        ebftBlock.getConsensusMessages().add(consensus);
        this.isConsensused = true;

        log.debug("make Consensus: [{}][{}]({})",
                ebftBlock.getIndex(), ebftBlock.getHash(), Hex.toHexString(consensus.toByteArray()));

        return ebftBlock;

    }

    private boolean checkReceiveProposedEbftBlock() {
        long index = this.blockChain.getBlockChainManager().getLastIndex() + 1;
        List<String> proposedAddr = new ArrayList<>();
        for (EbftBlock proposedEbftBlock : this.blockChain.getUnConfirmedData().values()) {
            if (proposedEbftBlock.getIndex() == index) {
                proposedAddr.add(proposedEbftBlock.getBlock().getAddress().toString());
            }
        }

        for (EbftClientStub client : this.totalValidatorMap.values()) {
            if (client.isRunning()
                    && !proposedAddr.contains(client.getAddr())) {
                return false;
            }
        }

        return true;
    }

    private int getUnconfirmedEbftBlockCount(Map<String, EbftBlock> unConfirmedEbftBlockMap, long index) {
        int count = 0;
        for (EbftBlock ebftBlock : unConfirmedEbftBlockMap.values()) {
            if (ebftBlock.getIndex() == index) {
                count++;
            }
        }
        return count;
    }

    private EbftBlock confirmFinalBlock() {
        if (!isConsensused) {
            return null;
        }

        long lastConfirmedBlockIndex = this.blockChain.getBlockChainManager().getLastIndex();
        for (String key : this.blockChain.getUnConfirmedData().keySet()) {
            EbftBlock unconfirmedBlock = this.blockChain.getUnConfirmedData().get(key);
            if (unconfirmedBlock == null) {
                this.blockChain.getUnConfirmedData().remove(key);
            } else if (unconfirmedBlock.getIndex() <= lastConfirmedBlockIndex) {
                unconfirmedBlock.clear();
                this.blockChain.getUnConfirmedData().remove(key);
            } else if (unconfirmedBlock.getIndex()
                    == lastConfirmedBlockIndex + 1
                    && unconfirmedBlock.getConsensusMessages().size() >= consensusCount) {
                confirmedBlock(unconfirmedBlock);
                return unconfirmedBlock;
            }
        }

        return null;
    }

    private void confirmedBlock(EbftBlock ebftBlock) {
        this.blockChain.addBlock(ebftBlock);
        this.isProposed = false;
        this.isConsensused = false;
    }

    private void resetUnConfirmedBlock(long index) {
        for (String key : this.blockChain.getUnConfirmedData().keySet()) {
            EbftBlock unConfirmedBlock = this.blockChain.getUnConfirmedData().get(key);
            if (unConfirmedBlock.getIndex() <= index) {
                unConfirmedBlock.clear();
                this.blockChain.getUnConfirmedData().remove(key);
            }
        }
    }

    private void loggingStatus() {
        log.trace("loggingStatus");

        try {
            if (log.isDebugEnabled()) {
                log.debug("map size= {}", this.blockChain.getBlockChainManager().countOfBlocks());
                log.debug("proposedBlock size= "
                        + this.blockChain.getUnConfirmedData().size());
                log.debug("isSynced= {}", isSynced);
                log.debug("isProposed= {}", isProposed);
                log.debug("isConsensused= {}", isConsensused);
                for (String key : this.blockChain.getUnConfirmedData().keySet()) {
                    EbftBlock ebftBlock = this.blockChain.getUnConfirmedData().get(key);
                    if (ebftBlock == null) {
                        break;
                    }
                    log.debug("proposed ["
                            + ebftBlock.getIndex()
                            + "]"
                            + ebftBlock.getHash()
                            + " ("
                            + ebftBlock.getBlock().getAddress()
                            + ")");
                    for (int i = 0; i < ebftBlock.getConsensusMessages().size(); i++) {
                        if (ebftBlock.getConsensusMessages().get(i) != null) {
                            log.debug(Hex.toHexString(ebftBlock.getConsensusMessages().get(i).toByteArray())
                                    + " ("
                                    + ebftBlock.getBlock().getAddress()
                                    + ")");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug(e.getMessage());
        }

        log.debug("");
    }

    private void multicastBlock(EbftBlock block) {
        for (Map.Entry<String, EbftClientStub> entry : totalValidatorMap.entrySet()) {
            EbftClientStub client = entry.getValue();
            if (client.isMyclient()) {
                continue;
            }
            if (client.isRunning()) {
                try {
                    client.multicastEbftBlock(block.getInstance());
                } catch (Exception e) {
                    log.debug("multicast exception: {}", e.getMessage());
                    log.debug("client: {}", client.getId());
                    log.debug("block: {}", block.getHash());
                }
            }
        }
    }

    private void broadcastBlock(EbftBlock block, Map<String, EbftClientStub> clientMap) {
        for (Map.Entry<String, EbftClientStub> entry : clientMap.entrySet()) {
            EbftClientStub client = entry.getValue();
            if (client.isMyclient()) {
                continue;
            }
            try {
                client.broadcastEbftBlock(block.getInstance());
                log.debug("BroadcastBlock [{}]{} to {}:{}", block.getIndex(), block.getHash(),
                        client.getHost(), client.getPort());
            } catch (Exception e) {
                log.debug("BroadcastBlock exception: {}", e.getMessage());
                log.debug("client: {}", client.getId());
                log.debug("block: {}", block.getHash());
            }
        }
    }

    void updateUnconfirmedBlock(EbftBlock ebftBlock) {
        if (ebftBlock == null) {
            return;
        }

        String addr = ebftBlock.getBlock().getAddress().toString();
        EbftBlock unConfirmedBlock =
                this.blockChain.getUnConfirmedData().get(ebftBlock.getHash().toString());

        if (unConfirmedBlock != null) {
            // if exist, update consensus
            if (!ebftBlock.getConsensusMessages().isEmpty()) {
                for (ByteString consensus : ebftBlock.getConsensusMessages()) {
                    if (!unConfirmedBlock.getConsensusMessages().contains(consensus)
                            && this.totalValidatorMap.containsKey(addr)) {
                        unConfirmedBlock.getConsensusMessages().add(consensus);
                    }
                }
            }
        } else {
            // if not exist, add ebftBlock
            this.blockChain.getUnConfirmedData().put(ebftBlock.getHash().toString(), ebftBlock);
        }
    }

    EbftStatus getMyNodeStatus() {
        long index = this.blockChain.getBlockChainManager().getLastIndex();
        List<EbftBlock> unConfirmedBlockList = new ArrayList<>();
        for (EbftBlock ebftBlock : this.blockChain.getUnConfirmedData().values()) {
            if (ebftBlock != null && ebftBlock.getBlock() != null
                    && ebftBlock.getIndex() == index + 1) {
                unConfirmedBlockList.add(ebftBlock);
            }
        }
        return new EbftStatus(index, unConfirmedBlockList, wallet);
    }

    private void printInitInfo() {
        log.info("Node Started");
        log.info("wallet address: {}", wallet.getHexAddress());
        log.info("wallet pubKey: {}", Hex.toHexString(wallet.getPubicKey()));
        log.info("isValidator: {}", this.isValidator);
    }

    @SuppressWarnings("unchecked")
    private TreeMap<String, EbftClientStub> initTotalValidator() {
        TreeMap<String, EbftClientStub> nodeMap = new TreeMap<>();
        try {
            Map<String, Object> validatorInfoMap =
                    this.defaultConfig.getConfig().getConfig(Constants.VALIDATOR_INFO).root().unwrapped();
            for (Map.Entry<String, Object> entry : validatorInfoMap.entrySet()) {
                String host = ((Map<String, String>) entry.getValue()).get("host");
                int port = ((Map<String, Integer>) entry.getValue()).get("port");
                EbftClientStub client = new EbftClientStub(entry.getKey(), host, port);
                if (client.getId().equals(myNode.getId())) {
                    nodeMap.put(myNode.getAddr(), myNode);
                } else {
                    nodeMap.put(client.getAddr(), client);
                }
            }
            log.debug("ValidatorInfo: {}", nodeMap);
        } catch (ConfigException ce) {
            throw new NotValidateException("Validators is not set.");
        }
        return nodeMap;
    }

    @SuppressWarnings("unchecked")
    private TreeMap<String, EbftClientStub> initProxyNode() {
        TreeMap<String, EbftClientStub> nodeMap = new TreeMap<>();
        try {
            Map<String, Object> proxyNodeInfo =
                    this.defaultConfig.getConfig().getConfig(Constants.VALIDATOR_PROXYNODE).root().unwrapped();
            for (Map.Entry<String, Object> entry : proxyNodeInfo.entrySet()) {
                String host = ((Map<String, String>) entry.getValue()).get("host");
                int port = ((Map<String, Integer>) entry.getValue()).get("port");
                EbftClientStub client = new EbftClientStub(entry.getKey(), host, port);
                if (client.getId().equals(myNode.getId())) {
                    nodeMap.put(myNode.getAddr(), myNode);
                } else {
                    nodeMap.put(client.getAddr(), client);
                }
            }
            log.debug("ProxyNode: {}", nodeMap);
        } catch (ConfigException ce) {
            log.warn("ProxyNode is not set.");
        }
        return nodeMap;
    }

    private EbftClientStub initMyNode() {
        EbftClientStub client = new EbftClientStub(
                wallet.getHexAddress(), this.grpcHost, this.grpcPort);
        client.setMyclient(true);
        client.setIsRunning(true);
        return client;
    }

    private boolean initValidator() {
        log.debug("MyNode ID: {}", myNode.getId());
        for (EbftClientStub clientStub : totalValidatorMap.values()) {
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

        log.debug("running node: {}", runningNodeCount);
    }

    private int getActiveNodeCount() {
        int count = 0;
        for (EbftClientStub client : totalValidatorMap.values()) {
            if (client.isRunning()) {
                count++;
            }
        }
        return count;
    }

    boolean consensusVerify(EbftBlock ebftBlock) {
        if (ebftBlock.getConsensusMessages().isEmpty()) {
            return true;
        }

        for (ByteString signature : ebftBlock.getConsensusMessages()) {
            if (!Wallet.verify(ebftBlock.getHash().getBytes(), signature.toByteArray(), true)) {
                return false;
            }
            // todo: else, check validator
        }

        return true;
    }

    // todo: check security
    @Override
    public ReentrantLock getLock() {
        return lock;
    }

    @Override
    public ConsensusBlockChain<EbftProto.EbftBlock, EbftBlock> getBlockChain() {
        return blockChain;
    }

    @Override
    public Map<String, EbftClientStub> getTotalValidatorMap() {
        return totalValidatorMap;
    }
}
