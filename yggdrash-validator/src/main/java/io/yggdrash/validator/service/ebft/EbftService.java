package io.yggdrash.validator.service.ebft;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockBody;
import io.yggdrash.core.blockchain.BlockHeader;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.data.ConsensusBlockChain;
import io.yggdrash.validator.data.ebft.EbftBlock;
import io.yggdrash.validator.data.ebft.EbftBlockChain;
import io.yggdrash.validator.data.ebft.EbftStatus;
import io.yggdrash.validator.service.ConsensusService;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class EbftService implements ConsensusService {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(EbftService.class);

    private static final boolean ABNORMAL_TEST = false;

    private final boolean isValidator;
    private final int consensusCount;

    private final Wallet wallet;
    private final EbftBlockChain blockChain;

    private final EbftClientStub myNode;
    private final Map<String, EbftClientStub> totalValidatorMap;

    private final ReentrantLock lock = new ReentrantLock();

    private boolean isActive;
    private boolean isSynced;
    private boolean isProposed;
    private boolean isConsensused;

    private String grpcHost;
    private int grpcPort;

    public EbftService(Wallet wallet, ConsensusBlockChain blockChain, String grpcHost, int grpcPort) {
        this.wallet = wallet;
        this.blockChain = (EbftBlockChain) blockChain;
        this.grpcHost = grpcHost;
        this.grpcPort = grpcPort;

        this.myNode = initMyNode();
        this.totalValidatorMap = initTotalValidator();
        this.isValidator = initValidator();
        this.isActive = false;
        this.isSynced = false;
        if (totalValidatorMap != null) {
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

    public void mainScheduler() {
        if (!isValidator) {
            log.info("Node is not validator.");
            return;
        }

        loggingStatus();

        checkNode();

        if (!isActive) {
            log.info("Validator is not active.");
            return;
        }

        lock.lock();
        EbftBlock proposedEbftBlock = makeProposedBlock();
        lock.unlock();
        if (proposedEbftBlock != null) {
            multicast(proposedEbftBlock.clone());
            if (!waitingProposedBlock()) {
                log.debug("ProposedBlock count is not enough.");
            }
        }

        lock.lock();
        EbftBlock consensusedEbftBlock = makeConsensus();
        lock.unlock();
        if (consensusedEbftBlock != null) {
            multicast(consensusedEbftBlock.clone());
            if (!waitingConsensusedBlock()) {
                log.debug("ConsensusedBlock count is not enough.");
            }
        }

        lock.lock();
        confirmFinalBlock();
        lock.unlock();
    }

    private boolean waitingProposedBlock() {
        for (int i = 0; i < consensusCount; i++) {
            if (getUnconfirmedEbftBlockCount(blockChain.getUnConfirmedData(),
                    blockChain.getLastConfirmedBlock().getIndex() + 1)
                    < consensusCount) {
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

    private boolean waitingConsensusedBlock() {
        for (int i = 0; i < consensusCount; i++) {
            for (EbftBlock unConfirmedEbftBlock : blockChain.getUnConfirmedData().values()) {
                if (unConfirmedEbftBlock.getIndex() ==
                        blockChain.getLastConfirmedBlock().getIndex() + 1
                        && unConfirmedEbftBlock.getConsensusMessages().size() > consensusCount) {
                    return true;
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.trace(e.getMessage());
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

        if (ebftStatus.getIndex()
                > this.blockChain.getLastConfirmedBlock().getIndex()) {
            log.debug("this Index: "
                    + this.blockChain.getLastConfirmedBlock().getIndex());
            log.debug("client Index: " + ebftStatus.getIndex());
            log.debug("client : " + client.getId());

            this.isSynced = false;
            blockSyncing(client.getAddr(), ebftStatus.getIndex());
        } else if (ebftStatus.getIndex()
                == this.blockChain.getLastConfirmedBlock().getIndex()) {
            for (EbftBlock ebftBlock : ebftStatus.getUnConfirmedEbftBlockList()) {
                updateUnconfirmedBlock(ebftBlock);
            }
        }
    }

    private void blockSyncing(String addr, long index) {
        EbftClientStub client = totalValidatorMap.get(addr);
        log.debug("node: " + client.getId());
        log.debug("index: " + index);
        if (!client.isRunning()) {
            return;
        }

        List<EbftBlock> ebftBlockList = new ArrayList<>(client.getEbftBlockList(
                this.blockChain.getLastConfirmedBlock().getIndex()));
        log.debug("ebftBlockList size: " + ebftBlockList.size());
        if (ebftBlockList.size() == 0) {
            return;
        }

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

        if (this.blockChain.getLastConfirmedBlock().getIndex() < index) {
            blockSyncing(addr, index);
        }
    }

    private EbftBlock makeProposedBlock() {
        if (this.isProposed
                || !this.isSynced) {
            return null;
        }

        long index = this.blockChain.getLastConfirmedBlock().getIndex() + 1;
        byte[] prevBlockHash = this.blockChain.getLastConfirmedBlock().getHash();

        Block newBlock = makeNewBlock(index, prevBlockHash);
        log.trace("newBlock" + newBlock.toString());

        EbftBlock newEbftBlock = new EbftBlock(newBlock);

        // add in unconfirmed blockMap & unconfirmed block
        this.blockChain.getUnConfirmedData()
                .putIfAbsent(newEbftBlock.getHashHex(), newEbftBlock);
        this.isProposed = true;

        log.debug("make Proposed Block"
                + "["
                + newEbftBlock.getIndex()
                + "]"
                + newEbftBlock.getHashHex()
                + " ("
                + newEbftBlock.getBlock().getAddressHex()
                + ")");

        return newEbftBlock;
    }

    private Block makeNewBlock(long index, byte[] prevBlockHash) {
        List<Transaction> txs = new ArrayList<>();
        List<TransactionHusk> txHusks = new ArrayList<>(
                blockChain.getTransactionStore().getUnconfirmedTxs());
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

    private EbftBlock makeConsensus() {
        if (this.isConsensused || !this.isSynced) {
            return null;
        }

        Map<String, EbftBlock> unConfirmedEbftBlockMap =
                this.blockChain.getUnConfirmedData();
        int unconfirmedEbftBlockCount = getUnconfirmedEbftBlockCount(
                unConfirmedEbftBlockMap,
                this.blockChain.getLastConfirmedBlock().getIndex() + 1);

        //todo : check active count, active status
        if (unconfirmedEbftBlockCount < getActiveNodeCount()
                || unconfirmedEbftBlockCount < consensusCount
                || !checkReceiveProposedEbftBlock()) {
            log.debug("Cannot makeConsensus: "
                    + "unConfirmedBlockCount: " + unconfirmedEbftBlockCount
                    + " getActiveNodeCount: " + getActiveNodeCount()
                    + " checkReceiveProposedEbftBlock: " + checkReceiveProposedEbftBlock()
            );
            return null;
        }

        //todo: check efficiency
        String minKey = null;
        for (String key : unConfirmedEbftBlockMap.keySet()) {
            if (unConfirmedEbftBlockMap.get(key).getIndex()
                    != this.blockChain.getLastConfirmedBlock().getIndex() + 1) {
                continue;
            }
            if (minKey == null) {
                minKey = key;
                if (ABNORMAL_TEST) {
                    // for test abnormal node(attacker)
                    break;
                }
            } else {
                if (org.spongycastle.util.Arrays.compareUnsigned(Hex.decode(minKey),
                        Hex.decode(key)) > 0) {
                    minKey = key;
                }
            }
        }

        EbftBlock ebftBlock = unConfirmedEbftBlockMap.get(minKey);
        String consensus = wallet.signHex(ebftBlock.getHash(), true);
        ebftBlock.getConsensusMessages().add(consensus);
        this.isConsensused = true;

        log.debug("make Consensus: "
                + "["
                + ebftBlock.getIndex()
                + "] "
                + ebftBlock.getHashHex()
                + " ("
                + consensus
                + ")");

        return ebftBlock;

    }

    private boolean checkReceiveProposedEbftBlock() {
        long index = this.blockChain.getLastConfirmedBlock().getIndex() + 1;
        List<String> proposedAddr = new ArrayList<>();
        for (EbftBlock proposedEbftBlock : this.blockChain.getUnConfirmedData().values()) {
            if (proposedEbftBlock.getIndex() == index) {
                proposedAddr.add(proposedEbftBlock.getBlock().getAddressHex());
            }
        }

        for (EbftClientStub client : this.totalValidatorMap.values()) {
            if (client.isRunning()) {
                if (!proposedAddr.contains(client.getAddr())) {
                    return false;
                }
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

    private void confirmFinalBlock() {
        boolean moreConfirmFlag = false;
        for (String key : this.blockChain.getUnConfirmedData().keySet()) {
            EbftBlock unconfirmedBlock = this.blockChain.getUnConfirmedData().get(key);
            if (unconfirmedBlock == null) {
                this.blockChain.getUnConfirmedData().remove(key);
                continue;
            } else if (unconfirmedBlock.getIndex()
                    <= this.blockChain.getLastConfirmedBlock().getIndex()) {
                unconfirmedBlock.clear();
                this.blockChain.getUnConfirmedData().remove(key);
            } else if (unconfirmedBlock.getIndex()
                    == this.blockChain.getLastConfirmedBlock().getIndex() + 1
                    && unconfirmedBlock.getConsensusMessages().size() >= consensusCount) {
                confirmedBlock(unconfirmedBlock);
            } else if (unconfirmedBlock.getConsensusMessages().size() >= consensusCount) {
                moreConfirmFlag = true;
            }
        }

        if (moreConfirmFlag) {
            confirmFinalBlock();
        }
    }

    private void confirmedBlock(EbftBlock ebftBlock) {
        this.blockChain.addBlock(ebftBlock);
        resetUnConfirmedBlock(ebftBlock.getIndex());
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
            EbftBlock lastBlock = this.blockChain.getLastConfirmedBlock();
            if (lastBlock != null) {
                log.info("EbftBlock [" + lastBlock.getIndex() + "] "
                        + lastBlock.getHashHex()
                        + " ("
                        + lastBlock.getBlock().getAddressHex()
                        + ") "
                        + "("
                        + lastBlock.getConsensusMessages().size()
                        + ")");
            }

            if (log.isDebugEnabled()) {
                log.debug("map size= " + this.blockChain.getBlockStore().size());
                log.debug("key size= " + this.blockChain.getBlockKeyStore().size());
                log.debug("proposedBlock size= "
                        + this.blockChain.getUnConfirmedData().size());
                log.debug("isSynced= " + isSynced);
                log.debug("isProposed= " + this.isProposed);
                log.debug("isConsensused= " + this.isConsensused);
                for (String key : this.blockChain.getUnConfirmedData().keySet()) {
                    EbftBlock ebftBlock = this.blockChain.getUnConfirmedData().get(key);
                    if (ebftBlock == null) {
                        break;
                    }
                    log.debug("proposed ["
                            + ebftBlock.getIndex()
                            + "]"
                            + ebftBlock.getHashHex()
                            + " ("
                            + ebftBlock.getBlock().getAddressHex()
                            + ")");
                    for (int i = 0; i < ebftBlock.getConsensusMessages().size(); i++) {
                        if (ebftBlock.getConsensusMessages().get(i) != null) {
                            log.debug(ebftBlock.getConsensusMessages().get(i)
                                    + " ("
                                    + ebftBlock.getBlock().getAddressHex()
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

    private void multicast(EbftBlock ebftBlock) {
        for (String key : totalValidatorMap.keySet()) {
            EbftClientStub client = totalValidatorMap.get(key);
            if (client.isMyclient()) {
                continue;
            }
            if (client.isRunning()) {
                try {
                    client.multicastEbftBlock(EbftBlock.toProto(ebftBlock));
                } catch (Exception e) {
                    log.debug("multicast exception: " + e.getMessage());
                    log.debug("client: " + client.getId());
                    log.debug("ebftBlock: " + ebftBlock.getHashHex());
                    // continue
                }
            }
        }
    }

    public void updateUnconfirmedBlock(EbftBlock ebftBlock) {
        if (ebftBlock == null) {
            return;
        }

        String addr = ebftBlock.getBlock().getAddressHex();
        EbftBlock unConfirmedBlock =
                this.blockChain.getUnConfirmedData().get(ebftBlock.getHashHex());

        if (unConfirmedBlock != null) {
            // if exist, update consensus
            if (ebftBlock.getConsensusMessages().size() > 0) {
                for (String consensus : ebftBlock.getConsensusMessages()) {
                    if (!unConfirmedBlock.getConsensusMessages().contains(consensus)
                            && this.totalValidatorMap.containsKey(addr)) {
                        unConfirmedBlock.getConsensusMessages().add(consensus);
                    }
                }
            }
        } else {
            // if not exist, add ebftBlock
            this.blockChain.getUnConfirmedData().put(ebftBlock.getHashHex(), ebftBlock);
        }
    }

    public EbftStatus getMyNodeStatus() {
        EbftStatus newEbftStatus =
                new EbftStatus(this.blockChain.getLastConfirmedBlock().getIndex(),
                        new ArrayList<>(this.blockChain.getUnConfirmedData().values()));
        newEbftStatus.setSignature(wallet.sign(newEbftStatus.getHashForSigning(), true));
        return newEbftStatus;
    }

    private void printInitInfo() {
        log.info("Node Started");
        log.info("wallet address: " + wallet.getHexAddress());
        log.info("wallet pubKey: " + Hex.toHexString(wallet.getPubicKey()));
        log.info("isValidator: " + this.isValidator);
    }

    private Map<String, EbftClientStub> initTotalValidator() {
        String jsonString;
        ClassPathResource cpr = new ClassPathResource("validator-config.json");
        try {
            byte[] bdata = FileCopyUtils.copyToByteArray(cpr.getInputStream());
            jsonString = new String(bdata, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.debug("Error validator.json");
            return null;
        }

        JsonObject validatorJsonObject = new Gson().fromJson(jsonString, JsonObject.class);
        Map<String, EbftClientStub> nodeMap = new ConcurrentHashMap<>();

        Set<Map.Entry<String, JsonElement>> entrySet =
                validatorJsonObject.get("validator").getAsJsonObject().entrySet();
        for (Map.Entry<String, JsonElement> entry : entrySet) {
            EbftClientStub client = new EbftClientStub(entry.getKey(),
                    entry.getValue().getAsJsonObject().get("host").getAsString(),
                    entry.getValue().getAsJsonObject().get("port").getAsInt());
            if (client.getId().equals(myNode.getId())) {
                nodeMap.put(myNode.getAddr(), myNode);
            } else {
                nodeMap.put(client.getAddr(), client);
            }
        }

        log.debug("isValidator" + validatorJsonObject.toString());
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
        log.debug("MyNode ID: " + this.myNode.getId());
        return totalValidatorMap.containsKey(this.myNode.getAddr());
    }

    private List<String> getActiveNodeList() {
        List<String> activeNodeList = new ArrayList<>();
        for (EbftClientStub client : totalValidatorMap.values()) {
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
        for (EbftClientStub client : totalValidatorMap.values()) {
            if (client.isRunning()) {
                count++;
            }
        }
        return count;
    }

    public boolean consensusVerify(EbftBlock ebftBlock) {
        if (ebftBlock.getConsensusMessages().size() <= 0) {
            return true;
        }

        for (String signature : ebftBlock.getConsensusMessages()) {
            if (!Wallet.verify(ebftBlock.getHash(), Hex.decode(signature), true)) {
                return false;
            }
            // todo: else, check validator
        }

        return true;
    }

    // todo: check security
    public ReentrantLock getLock() {
        return lock;
    }
}
