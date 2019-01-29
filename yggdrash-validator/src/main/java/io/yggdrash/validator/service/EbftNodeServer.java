package io.yggdrash.validator.service;

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
import io.yggdrash.validator.data.BlockCon;
import io.yggdrash.validator.data.BlockConChain;
import io.yggdrash.validator.data.NodeStatus;
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
public class EbftNodeServer implements CommandLineRunner {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(EbftNodeServer.class);

    private static final boolean ABNORMAL_TEST = false;

    private final boolean isValidator;
    private final int consenusCount;

    private final Wallet wallet;
    private final BlockConChain blockConChain;

    private final EbftNodeClient myNode;
    private final Map<String, EbftNodeClient> totalValidatorMap;

    private boolean isActive;
    private boolean isSynced;

    private ReentrantLock lock = new ReentrantLock();

    @Autowired
    public EbftNodeServer(Wallet wallet, BlockConChain blockConChain) {
        this.wallet = wallet;
        this.blockConChain = blockConChain;
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
        BlockCon proposedBlockCon = makeProposedBlock();
        lock.unlock();
        if (proposedBlockCon != null) {
            broadcast(proposedBlockCon.clone());
        }

        sleep(500);

        lock.lock();
        BlockCon consensusedBlockCon = makeConsensus();
        lock.unlock();
        if (consensusedBlockCon != null) {
            broadcast(consensusedBlockCon.clone());
        }

        sleep(500);

        lock.lock();
        confirmFinalBlock();
        lock.unlock();

        loggingNode();

    }

    private void checkNode() {
        for (String key : totalValidatorMap.keySet()) {
            EbftNodeClient client = totalValidatorMap.get(key);
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

    private void checkNodeStatus(EbftNodeClient client) {
        NodeStatus nodeStatus = client.exchangeNodeStatus(NodeStatus.toProto(getMyNodeStatus()));
        updateStatus(client, nodeStatus);
    }

    private void updateStatus(EbftNodeClient client, NodeStatus nodeStatus) {
        if (NodeStatus.verify(nodeStatus)) {
            client.setIsRunning(true);

            if (nodeStatus.getLastConfirmedBlockCon().getIndex()
                    > this.blockConChain.getLastConfirmedBlockCon().getIndex()) {
                log.debug("this Index: "
                        + this.blockConChain.getLastConfirmedBlockCon().getIndex());
                log.debug("client Index: " + nodeStatus.getLastConfirmedBlockCon().getIndex());
                log.debug("client : " + client.getId());

                // blockConSyncing
                this.isSynced = false;
                blockConSyncing(client.getPubKey(),
                        nodeStatus.getLastConfirmedBlockCon().getIndex());
            } else if (nodeStatus.getLastConfirmedBlockCon().getIndex()
                    == this.blockConChain.getLastConfirmedBlockCon().getIndex()) {
                // unconfirmed block update
                for (BlockCon blockCon : nodeStatus.getUnConfirmedBlockConList()) {
                    updateUnconfirmedBlock(blockCon);
                }
            }
        } else {
            client.setIsRunning(false);
        }
    }

    private void blockConSyncing(String pubKey, long index) {
        EbftNodeClient client = totalValidatorMap.get(pubKey);
        BlockCon blockCon;
        if (client.isRunning()) {
            List<BlockCon> blockConList = new ArrayList<>(client.getBlockConList(
                    this.blockConChain.getLastConfirmedBlockCon().getIndex()));

            log.debug("node: " + client.getId());
            log.debug("index: " + index);
            log.debug("blockConList size: " + blockConList.size());

            if (blockConList.size() == 0) {
                return;
            }

            int i = 0;
            for (; i < blockConList.size(); i++) {
                blockCon = blockConList.get(i);
                if (!BlockCon.verify(blockCon) || !consensusVerify(blockCon)) {
                    log.error("blockConSyncing Verify Fail");
                    continue;
                }
                this.blockConChain.getBlockConStore().put(blockCon.getHash(), blockCon);
                this.blockConChain.getBlockConKeyStore()
                        .put(blockCon.getIndex(), blockCon.getHash());
            }
            blockCon = blockConList.get(i - 1);
            //todo: if consensusCount(validator count) is different from previous count,
            // cannot confirm prevBlockCon.
            if (blockCon.getConsensusList().size() >= consenusCount) {
                changeLastConfirmedBlock(blockCon);
                this.blockConChain.setProposed(false);
                this.blockConChain.setConsensused(false);
            }
        }

        if (this.blockConChain.getLastConfirmedBlockCon().getIndex() < index) {
            blockConSyncing(pubKey, index);
        }
    }

    private BlockCon makeProposedBlock() {
        if (this.isValidator
                && this.isActive
                && !this.blockConChain.isProposed()
                && this.isSynced) {
            long index = this.blockConChain.getLastConfirmedBlockCon().getIndex() + 1;
            byte[]  prevBlockHash = this.blockConChain.getLastConfirmedBlockCon().getHash();

            Block newBlock = makeNewBlock(index, prevBlockHash);
            log.trace("newBlock" + newBlock.toString());

            BlockCon newBlockCon
                    = new BlockCon(index, prevBlockHash, newBlock);

            // add in unconfirmed blockConMap & unconfirmed blockCon
            this.blockConChain.getUnConfirmedBlockConMap()
                    .putIfAbsent(newBlockCon.getHashHex(), newBlockCon);
            this.blockConChain.setProposed(true);

            log.debug("make Proposed Block"
                    + "["
                    + newBlockCon.getIndex()
                    + "]"
                    + newBlockCon.getHashHex()
                    + " ("
                    + newBlockCon.getBlock().getAddressHexString()
                    + ")");

            return newBlockCon;
        }

        return null;
    }

    private Block makeNewBlock(long index, byte[] prevBlockHash) {
        List<Transaction> txs = new ArrayList<>();
        List<TransactionHusk> txHusks = new ArrayList<>(
                blockConChain.getTransactionStore().getUnconfirmedTxs());
        for (TransactionHusk txHusk : txHusks) {
            txs.add(txHusk.getCoreTransaction());
        }

        BlockBody newBlockBody = new BlockBody(txs);
        BlockHeader newBlockHeader = new BlockHeader(
                blockConChain.getChain(),
                new byte[8],
                new byte[8],
                prevBlockHash,
                index,
                TimeUtils.time(),
                newBlockBody);
        return new Block(newBlockHeader, wallet, newBlockBody);
    }

    private BlockCon makeConsensus() {
        if (this.blockConChain.isConsensused() || !this.isSynced) {
            return null;
        }

        Map<String, BlockCon> unConfirmedBlockConMap =
                this.blockConChain.getUnConfirmedBlockConMap();
        int unconfirmedBlockConCount = getUnconfirmedBlockConCount(
                unConfirmedBlockConMap,
                this.blockConChain.getLastConfirmedBlockCon().getIndex() + 1);
        if (unconfirmedBlockConCount >= getActiveNodeCount()
                && unconfirmedBlockConCount >= consenusCount
                && checkReceiveProposedBlockCon()) { //todo: check efficiency

            String minKey = null;
            for (String key : unConfirmedBlockConMap.keySet()) {
                if (unConfirmedBlockConMap.get(key).getIndex()
                        != this.blockConChain.getLastConfirmedBlockCon().getIndex() + 1) {
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

            BlockCon blockCon = unConfirmedBlockConMap.get(minKey);
            String consensus = Hex.toHexString(wallet.signHashedData(blockCon.getHash()));
            blockCon.getConsensusList().add(consensus);
            this.blockConChain.setConsensused(true);

            log.debug("make Consensus: "
                    + "["
                    + blockCon.getIndex()
                    + "] "
                    + blockCon.getHashHex()
                    + " ("
                    + consensus
                    + ")");

            return blockCon;
        }

        return null;
    }

    private boolean checkReceiveProposedBlockCon() {
        long index = this.blockConChain.getLastConfirmedBlockCon().getIndex() + 1;
        List<String> proposedPubkey = new ArrayList<>();
        for (String key : this.blockConChain.getUnConfirmedBlockConMap().keySet()) {
            BlockCon proposedBlockCon = this.blockConChain.getUnConfirmedBlockConMap().get(key);

            if (proposedBlockCon.getIndex() == index) {
                proposedPubkey.add(Hex.toHexString(proposedBlockCon.getBlock().getPubKey()));
            }
        }

        for (String key : this.totalValidatorMap.keySet()) {
            EbftNodeClient client = this.totalValidatorMap.get(key);
            if (client.isRunning()) {
                if (!proposedPubkey.contains("04" + client.getPubKey())) {
                    return false;
                }
            }
        }

        return true;
    }

    private int getUnconfirmedBlockConCount(Map<String, BlockCon> map, long index) {
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
        for (String key : this.blockConChain.getUnConfirmedBlockConMap().keySet()) {
            BlockCon unconfirmedNode = this.blockConChain.getUnConfirmedBlockConMap().get(key);
            if (unconfirmedNode == null) {
                continue;
            }
            if (unconfirmedNode.getIndex()
                    <= this.blockConChain.getLastConfirmedBlockCon().getIndex()) {
                this.blockConChain.getUnConfirmedBlockConMap().remove(key);
            } else if (unconfirmedNode.getIndex()
                    == this.blockConChain.getLastConfirmedBlockCon().getIndex() + 1) {
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

    private void confirmedBlock(BlockCon blockCon) {
        // add newBlockCon into blockConMap
        this.blockConChain.getBlockConStore().put(blockCon.getHash(), blockCon);
        this.blockConChain.getBlockConKeyStore().put(blockCon.getIndex(), blockCon.getHash());

        changeLastConfirmedBlock(blockCon);
        this.blockConChain.setProposed(false);
        this.blockConChain.setConsensused(false);

        log.debug("ConfirmedBlockCon="
                + "["
                + this.blockConChain.getLastConfirmedBlockCon().getIndex()
                + "]"
                + this.blockConChain.getLastConfirmedBlockCon().getHashHex()
                + "("
                + this.blockConChain.getLastConfirmedBlockCon().getConsensusList().size()
                + ")");
    }

    private void changeLastConfirmedBlock(BlockCon blockCon) {
        // change lastConfirmedBlockCon
        this.blockConChain.setLastConfirmedBlockCon(blockCon);

        // clear unConfirmedBlockCon
        for (String key : this.blockConChain.getUnConfirmedBlockConMap().keySet()) {
            BlockCon unConfirmedBlockCon = this.blockConChain.getUnConfirmedBlockConMap().get(key);
            if (unConfirmedBlockCon.getIndex() <= blockCon.getIndex()) {
                this.blockConChain.getUnConfirmedBlockConMap().remove(key);
            }
        }
    }

    private void loggingNode() {

        BlockCon lastBlockCon = this.blockConChain.getLastConfirmedBlockCon().clone();
        if (lastBlockCon != null) {
            log.info("[" + lastBlockCon.getIndex() + "] "
                    + lastBlockCon.getHashHex()
                    + " ("
                    + lastBlockCon.getBlock().getAddressHexString()
                    + ") "
                    + "("
                    + lastBlockCon.getConsensusList().size()
                    + ")");
        }

        if (log.isDebugEnabled()) {
            log.debug("map size= " + this.blockConChain.getBlockConStore().size());
            log.debug("key size= " + this.blockConChain.getBlockConKeyStore().size());
            log.debug("proposedBlock size= "
                    + this.blockConChain.getUnConfirmedBlockConMap().size());
            log.debug("isSynced= " + isSynced);
            log.debug("isProposed= " + this.blockConChain.isProposed());
            log.debug("isConsensused= " + this.blockConChain.isConsensused());
            for (String key : this.blockConChain.getUnConfirmedBlockConMap().keySet()) {
                BlockCon blockCon = this.blockConChain.getUnConfirmedBlockConMap().get(key);
                if (blockCon == null) {
                    break;
                }
                log.debug("proposed ["
                        + blockCon.getIndex()
                        + "]"
                        + blockCon.getHashHex()
                        + " ("
                        + blockCon.getBlock().getAddressHexString()
                        + ")");
                for (int i = 0; i < blockCon.getConsensusList().size(); i++) {
                    if (blockCon.getConsensusList().get(i) != null) {
                        log.debug(blockCon.getConsensusList().get(i)
                                + " ("
                                + Hex.toHexString(
                                Wallet.calculateAddress(
                                        Wallet.calculatePubKey(
                                                blockCon.getHash(),
                                                Hex.decode(blockCon.getConsensusList().get(i)),
                                                true)))
                                + ")");
                    }
                }
            }
        }

        log.info("");
    }

    private void broadcast(BlockCon blockCon) {
        for (String key : totalValidatorMap.keySet()) {
            EbftNodeClient client = totalValidatorMap.get(key);
            if (client.isMyclient()) {
                continue;
            }
            if (client.isRunning()) {
                try {
                    client.broadcastBlockCon(BlockCon.toProto(blockCon));
                } catch (Exception e) {
                    log.debug("broadcast exception: " + e.getMessage());
                    log.debug("client: " + client.getId());
                    log.debug("blockCon: " + blockCon.getHashHex());
                    // continue
                }
            }
        }
    }

    public void updateUnconfirmedBlock(BlockCon blockCon) {
        if (this.blockConChain.getUnConfirmedBlockConMap().containsKey(blockCon.getHashHex())) {
            // if exist, update consensus
            if (blockCon.getConsensusList().size() > 0) {
                for (String consensus : blockCon.getConsensusList()) {
                    String pubKey = Hex.toHexString(
                            Objects.requireNonNull(Wallet.calculatePubKey(
                                    blockCon.getHash(), Hex.decode(consensus), true)))
                            .substring(2);
                    if (!this.blockConChain.getUnConfirmedBlockConMap().get(blockCon.getHashHex())
                            .getConsensusList().contains(consensus)
                            && this.totalValidatorMap.containsKey(pubKey)) {
                        this.blockConChain.getUnConfirmedBlockConMap().get(blockCon.getHashHex())
                                .getConsensusList().add(consensus);
                    }
                }
            }
        } else {
            // if not exist, add blockCon
            this.blockConChain.getUnConfirmedBlockConMap().put(blockCon.getHashHex(), blockCon);
        }
    }

    public NodeStatus getMyNodeStatus() {
        NodeStatus newNodeStatus = new NodeStatus(this.getActiveNodeList(),
                this.blockConChain.getLastConfirmedBlockCon(),
                new ArrayList<>(this.blockConChain.getUnConfirmedBlockConMap().values()));
        newNodeStatus.setSignature(wallet.sign(newNodeStatus.getDataForSignning()));
        return newNodeStatus;
    }

    private void printInitInfo() {
        log.info("Node Started");
        log.info("wallet address: " + wallet.getHexAddress());
        log.info("wallet pubKey: " + Hex.toHexString(wallet.getPubicKey()));
        log.info("isValidator: " + this.isValidator);
    }

    private Map<String, EbftNodeClient> initTotalValidator() {
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
        Map<String, EbftNodeClient> nodeMap = new ConcurrentHashMap<>();

        Set<Map.Entry<String, JsonElement>> entrySet =
                validatorJsonObject.get("validator").getAsJsonObject().entrySet();
        for (Map.Entry<String, JsonElement> entry : entrySet) {
            EbftNodeClient client = new EbftNodeClient(entry.getKey(),
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

    private EbftNodeClient initMyNode() {
        EbftNodeClient client = new EbftNodeClient(
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
            EbftNodeClient client = totalValidatorMap.get(key);
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

    public boolean consensusVerify(BlockCon blockCon) {
        if (blockCon.getConsensusList().size() <= 0) {
            return true;
        }

        for (String signature : blockCon.getConsensusList()) {
            if (!Wallet.verify(blockCon.getHash(), Hex.decode(signature), true)) {
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
