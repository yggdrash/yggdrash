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
import io.yggdrash.validator.data.ebft.EbftBlock;
import io.yggdrash.validator.data.ebft.EbftBlockChain;
import io.yggdrash.validator.data.ebft.NodeStatus;
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

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static io.yggdrash.common.util.Utils.sleep;

@Service
@EnableScheduling
@ConditionalOnProperty(name = "yggdrash.validator.consensus.algorithm", havingValue = "ebft")
public class EbftService implements CommandLineRunner {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(EbftService.class);

    private static final boolean ABNORMAL_TEST = false;

    private final boolean isValidator;
    private final int consenusCount;

    private final Wallet wallet;
    private final EbftBlockChain ebftBlockChain;

    private final EbftClientStub myNode;
    private final Map<String, EbftClientStub> totalValidatorMap;

    private boolean isActive;
    private boolean isSynced;

    private ReentrantLock lock = new ReentrantLock();

    @Autowired
    public EbftService(Wallet wallet, EbftBlockChain ebftBlockChain) {
        this.wallet = wallet;
        this.ebftBlockChain = ebftBlockChain;
        this.myNode = initMyNode();
        this.totalValidatorMap = initTotalValidator();
        this.isValidator = initValidator();
        this.isActive = false;
        this.isSynced = false;
        if (totalValidatorMap != null) {
            this.consenusCount = totalValidatorMap.size() / 2 + 1;
        } else {
            this.consenusCount = 0;
            throw new NotValidateException();
        }
    }

    @Override
    public void run(String... args) {
        printInitInfo();
    }

    @Scheduled(cron = "*/2 * * * * *")
    public void mainScheduler() {

        checkNode();

        lock.lock();
        EbftBlock proposedEbftBlock = makeProposedBlock();
        lock.unlock();
        if (proposedEbftBlock != null) {
            broadcast(proposedEbftBlock.clone());
        }

        sleep(500);

        lock.lock();
        EbftBlock consensusedEbftBlock = makeConsensus();
        lock.unlock();
        if (consensusedEbftBlock != null) {
            broadcast(consensusedEbftBlock.clone());
        }

        sleep(500);

        lock.lock();
        confirmFinalBlock();
        lock.unlock();

        loggingNode();

    }

    private void checkNode() {
        for (String key : totalValidatorMap.keySet()) {
            EbftClientStub client = totalValidatorMap.get(key);
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
        NodeStatus nodeStatus = client.exchangeNodeStatus(NodeStatus.toProto(getMyNodeStatus()));
        updateStatus(client, nodeStatus);
    }

    private void updateStatus(EbftClientStub client, NodeStatus nodeStatus) {
        if (NodeStatus.verify(nodeStatus)) {
            client.setIsRunning(true);

            if (nodeStatus.getLastConfirmedEbftBlock().getIndex()
                    > this.ebftBlockChain.getLastConfirmedEbftBlock().getIndex()) {
                log.debug("this Index: "
                        + this.ebftBlockChain.getLastConfirmedEbftBlock().getIndex());
                log.debug("client Index: " + nodeStatus.getLastConfirmedEbftBlock().getIndex());
                log.debug("client : " + client.getId());

                // blockSyncing
                this.isSynced = false;
                blockSyncing(client.getPubKey(),
                        nodeStatus.getLastConfirmedEbftBlock().getIndex());
            } else if (nodeStatus.getLastConfirmedEbftBlock().getIndex()
                    == this.ebftBlockChain.getLastConfirmedEbftBlock().getIndex()) {
                // unconfirmed block update
                for (EbftBlock ebftBlock : nodeStatus.getUnConfirmedEbftBlockList()) {
                    updateUnconfirmedBlock(ebftBlock);
                }
            }
        } else {
            client.setIsRunning(false);
        }
    }

    private void blockSyncing(String pubKey, long index) {
        EbftClientStub client = totalValidatorMap.get(pubKey);
        EbftBlock ebftBlock;
        if (client.isRunning()) {
            List<EbftBlock> ebftBlockList = new ArrayList<>(client.getEbftBlockList(
                    this.ebftBlockChain.getLastConfirmedEbftBlock().getIndex()));

            log.debug("node: " + client.getId());
            log.debug("index: " + index);
            log.debug("ebftBlockList size: " + ebftBlockList.size());

            if (ebftBlockList.size() == 0) {
                return;
            }

            int i = 0;
            for (; i < ebftBlockList.size(); i++) {
                ebftBlock = ebftBlockList.get(i);
                if (!EbftBlock.verify(ebftBlock) || !consensusVerify(ebftBlock)) {
                    log.warn("Verify Fail");
                    //todo: check verify fail exception
                    continue;
                }
                this.ebftBlockChain.getEbftBlockStore().put(ebftBlock.getHash(), ebftBlock);
                this.ebftBlockChain.getEbftBlockKeyStore()
                        .put(ebftBlock.getIndex(), ebftBlock.getHash());
            }
            ebftBlock = ebftBlockList.get(i - 1);
            //todo: if consensusCount(validator count) is different from previous count,
            // cannot confirm prevBlock.
            if (ebftBlock.getConsensusList().size() >= consenusCount) {
                changeLastConfirmedBlock(ebftBlock);
                this.ebftBlockChain.setProposed(false);
                this.ebftBlockChain.setConsensused(false);
            }
        }

        if (this.ebftBlockChain.getLastConfirmedEbftBlock().getIndex() < index) {
            blockSyncing(pubKey, index);
        }
    }

    private EbftBlock makeProposedBlock() {
        if (this.isValidator
                && this.isActive
                && !this.ebftBlockChain.isProposed()
                && this.isSynced) {
            long index = this.ebftBlockChain.getLastConfirmedEbftBlock().getIndex() + 1;
            byte[] prevBlockHash = this.ebftBlockChain.getLastConfirmedEbftBlock().getHash();

            Block newBlock = makeNewBlock(index, prevBlockHash);
            log.trace("newBlock" + newBlock.toString());

            EbftBlock newEbftBlock
                    = new EbftBlock(index, prevBlockHash, newBlock);

            // add in unconfirmed blockMap & unconfirmed block
            this.ebftBlockChain.getUnConfirmedEbftBlockMap()
                    .putIfAbsent(newEbftBlock.getHashHex(), newEbftBlock);
            this.ebftBlockChain.setProposed(true);

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

        return null;
    }

    private Block makeNewBlock(long index, byte[] prevBlockHash) {
        List<Transaction> txs = new ArrayList<>();
        List<TransactionHusk> txHusks = new ArrayList<>(
                ebftBlockChain.getTransactionStore().getUnconfirmedTxs());
        for (TransactionHusk txHusk : txHusks) {
            txs.add(txHusk.getCoreTransaction());
        }

        BlockBody newBlockBody = new BlockBody(txs);
        BlockHeader newBlockHeader = new BlockHeader(
                ebftBlockChain.getChain(),
                new byte[8],
                new byte[8],
                prevBlockHash,
                index,
                TimeUtils.time(),
                newBlockBody);
        return new Block(newBlockHeader, wallet, newBlockBody);
    }

    private EbftBlock makeConsensus() {
        if (this.ebftBlockChain.isConsensused() || !this.isSynced) {
            return null;
        }

        Map<String, EbftBlock> unConfirmedEbftBlockMap =
                this.ebftBlockChain.getUnConfirmedEbftBlockMap();
        int unconfirmedEbftBlockCount = getUnconfirmedEbftBlockCount(
                unConfirmedEbftBlockMap,
                this.ebftBlockChain.getLastConfirmedEbftBlock().getIndex() + 1);
        if (unconfirmedEbftBlockCount >= getActiveNodeCount()
                && unconfirmedEbftBlockCount >= consenusCount
                && checkReceiveProposedEbftBlock()) { //todo: check efficiency

            String minKey = null;
            for (String key : unConfirmedEbftBlockMap.keySet()) {
                if (unConfirmedEbftBlockMap.get(key).getIndex()
                        != this.ebftBlockChain.getLastConfirmedEbftBlock().getIndex() + 1) {
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
            String consensus = Hex.toHexString(wallet.signHashedData(ebftBlock.getHash()));
            ebftBlock.getConsensusList().add(consensus);
            this.ebftBlockChain.setConsensused(true);

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

        return null;
    }

    private boolean checkReceiveProposedEbftBlock() {
        long index = this.ebftBlockChain.getLastConfirmedEbftBlock().getIndex() + 1;
        List<String> proposedPubkey = new ArrayList<>();
        for (String key : this.ebftBlockChain.getUnConfirmedEbftBlockMap().keySet()) {
            EbftBlock proposedEbftBlock = this.ebftBlockChain.getUnConfirmedEbftBlockMap().get(key);

            if (proposedEbftBlock.getIndex() == index) {
                proposedPubkey.add(Hex.toHexString(proposedEbftBlock.getBlock().getPubKey()));
            }
        }

        for (String key : this.totalValidatorMap.keySet()) {
            EbftClientStub client = this.totalValidatorMap.get(key);
            if (client.isRunning()) {
                if (!proposedPubkey.contains("04" + client.getPubKey())) {
                    return false;
                }
            }
        }

        return true;
    }

    private int getUnconfirmedEbftBlockCount(Map<String, EbftBlock> map, long index) {
        int count = 0;
        for (String key : map.keySet()) {
            if (map.get(key).getIndex() == index) {
                count++;
            }
        }
        return count;
    }

    private void confirmFinalBlock() {
        boolean moreConfirmFlag = false;
        for (String key : this.ebftBlockChain.getUnConfirmedEbftBlockMap().keySet()) {
            EbftBlock unconfirmedNode = this.ebftBlockChain.getUnConfirmedEbftBlockMap().get(key);
            if (unconfirmedNode == null) {
                continue;
            }
            if (unconfirmedNode.getIndex()
                    <= this.ebftBlockChain.getLastConfirmedEbftBlock().getIndex()) {
                this.ebftBlockChain.getUnConfirmedEbftBlockMap().remove(key);
            } else if (unconfirmedNode.getIndex()
                    == this.ebftBlockChain.getLastConfirmedEbftBlock().getIndex() + 1) {
                if (unconfirmedNode.getConsensusList().size() >= consenusCount) {
                    confirmedBlock(unconfirmedNode);
                }
            } else {
                if (unconfirmedNode.getConsensusList().size() >= consenusCount) {
                    moreConfirmFlag = true;
                }
            }
        }

        if (moreConfirmFlag) {
            confirmFinalBlock();
        }
    }

    private void confirmedBlock(EbftBlock ebftBlock) {
        // add newBlock into blockMap
        this.ebftBlockChain.getEbftBlockStore().put(ebftBlock.getHash(), ebftBlock);
        this.ebftBlockChain.getEbftBlockKeyStore().put(ebftBlock.getIndex(), ebftBlock.getHash());

        changeLastConfirmedBlock(ebftBlock);
        this.ebftBlockChain.setProposed(false);
        this.ebftBlockChain.setConsensused(false);

        log.debug("ConfirmedBlock="
                + "["
                + this.ebftBlockChain.getLastConfirmedEbftBlock().getIndex()
                + "]"
                + this.ebftBlockChain.getLastConfirmedEbftBlock().getHashHex()
                + "("
                + this.ebftBlockChain.getLastConfirmedEbftBlock().getConsensusList().size()
                + ")");
    }

    private void changeLastConfirmedBlock(EbftBlock ebftBlock) {
        // change lastConfirmedBlock
        this.ebftBlockChain.setLastConfirmedEbftBlock(ebftBlock);

        // clear unConfirmedBlock
        for (String key : this.ebftBlockChain.getUnConfirmedEbftBlockMap().keySet()) {
            EbftBlock unConfirmedEbftBlock = this.ebftBlockChain.getUnConfirmedEbftBlockMap().get(key);
            if (unConfirmedEbftBlock.getIndex() <= ebftBlock.getIndex()) {
                this.ebftBlockChain.getUnConfirmedEbftBlockMap().remove(key);
            }
        }
    }

    private void loggingNode() {

        EbftBlock lastEbftBlock = this.ebftBlockChain.getLastConfirmedEbftBlock().clone();
        if (lastEbftBlock != null) {
            log.info("[" + lastEbftBlock.getIndex() + "] "
                    + lastEbftBlock.getHashHex()
                    + " ("
                    + lastEbftBlock.getBlock().getAddressHex()
                    + ") "
                    + "("
                    + lastEbftBlock.getConsensusList().size()
                    + ")");
        }

        if (log.isDebugEnabled()) {
            log.debug("map size= " + this.ebftBlockChain.getEbftBlockStore().size());
            log.debug("key size= " + this.ebftBlockChain.getEbftBlockKeyStore().size());
            log.debug("proposedBlock size= "
                    + this.ebftBlockChain.getUnConfirmedEbftBlockMap().size());
            log.debug("isSynced= " + isSynced);
            log.debug("isProposed= " + this.ebftBlockChain.isProposed());
            log.debug("isConsensused= " + this.ebftBlockChain.isConsensused());
            for (String key : this.ebftBlockChain.getUnConfirmedEbftBlockMap().keySet()) {
                EbftBlock ebftBlock = this.ebftBlockChain.getUnConfirmedEbftBlockMap().get(key);
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
                for (int i = 0; i < ebftBlock.getConsensusList().size(); i++) {
                    if (ebftBlock.getConsensusList().get(i) != null) {
                        log.debug(ebftBlock.getConsensusList().get(i)
                                + " ("
                                + Hex.toHexString(
                                Wallet.calculateAddress(
                                        Wallet.calculatePubKey(
                                                ebftBlock.getHash(),
                                                Hex.decode(ebftBlock.getConsensusList().get(i)),
                                                true)))
                                + ")");
                    }
                }
            }
        }

        log.info("");
    }

    private void broadcast(EbftBlock ebftBlock) {
        for (String key : totalValidatorMap.keySet()) {
            EbftClientStub client = totalValidatorMap.get(key);
            if (client.isMyclient()) {
                continue;
            }
            if (client.isRunning()) {
                try {
                    client.broadcastEbftBlock(EbftBlock.toProto(ebftBlock));
                } catch (Exception e) {
                    log.debug("broadcast exception: " + e.getMessage());
                    log.debug("client: " + client.getId());
                    log.debug("ebftBlock: " + ebftBlock.getHashHex());
                    // continue
                }
            }
        }
    }

    public void updateUnconfirmedBlock(EbftBlock ebftBlock) {
        if (this.ebftBlockChain.getUnConfirmedEbftBlockMap().containsKey(ebftBlock.getHashHex())) {
            // if exist, update consensus
            if (ebftBlock.getConsensusList().size() > 0) {
                for (String consensus : ebftBlock.getConsensusList()) {
                    String pubKey = Hex.toHexString(
                            Objects.requireNonNull(Wallet.calculatePubKey(
                                    ebftBlock.getHash(), Hex.decode(consensus), true)))
                            .substring(2);
                    if (!this.ebftBlockChain.getUnConfirmedEbftBlockMap().get(ebftBlock.getHashHex())
                            .getConsensusList().contains(consensus)
                            && this.totalValidatorMap.containsKey(pubKey)) {
                        this.ebftBlockChain.getUnConfirmedEbftBlockMap().get(ebftBlock.getHashHex())
                                .getConsensusList().add(consensus);
                    }
                }
            }
        } else {
            // if not exist, add ebftBlock
            this.ebftBlockChain.getUnConfirmedEbftBlockMap().put(ebftBlock.getHashHex(), ebftBlock);
        }
    }

    public NodeStatus getMyNodeStatus() {
        NodeStatus newNodeStatus = new NodeStatus(this.getActiveNodeList(),
                this.ebftBlockChain.getLastConfirmedEbftBlock(),
                new ArrayList<>(this.ebftBlockChain.getUnConfirmedEbftBlockMap().values()));
        newNodeStatus.setSignature(wallet.sign(newNodeStatus.getDataForSignning()));
        return newNodeStatus;
    }

    private void printInitInfo() {
        log.info("Node Started");
        log.info("wallet address: " + wallet.getHexAddress());
        log.info("wallet pubKey: " + Hex.toHexString(wallet.getPubicKey()));
        log.info("isValidator: " + this.isValidator);
    }

    private Map<String, EbftClientStub> initTotalValidator() {
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
        Map<String, EbftClientStub> nodeMap = new ConcurrentHashMap<>();

        Set<Map.Entry<String, JsonElement>> entrySet =
                validatorJsonObject.get("validator").getAsJsonObject().entrySet();
        for (Map.Entry<String, JsonElement> entry : entrySet) {
            EbftClientStub client = new EbftClientStub(entry.getKey(),
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

    private EbftClientStub initMyNode() {
        EbftClientStub client = new EbftClientStub(
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
            EbftClientStub client = totalValidatorMap.get(key);
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

    public boolean consensusVerify(EbftBlock ebftBlock) {
        if (ebftBlock.getConsensusList().size() <= 0) {
            return true;
        }

        for (String signature : ebftBlock.getConsensusList()) {
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
