package io.yggdrash.validator.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.common.util.ByteUtil;
import io.yggdrash.common.util.TimeUtils;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.data.PbftBlock;
import io.yggdrash.validator.data.PbftBlockChain;
import io.yggdrash.validator.data.PbftStatus;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@EnableScheduling
@ConditionalOnProperty(name = "yggdrash.validator.consensus.algorithm", havingValue = "pbft")
public class PbftService implements CommandLineRunner {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(PbftService.class);

    private static final boolean ABNORMAL_TEST = false;

    private final boolean isValidator;
    private final int bftCount;
    private final int consenusCount;

    private final Wallet wallet;
    private final PbftBlockChain blockChain;

    private final PbftClientStub myNode;
    private final Map<String, PbftClientStub> totalValidatorMap;

    private boolean isActive;
    private boolean isSynced;

    private ReentrantLock lock = new ReentrantLock();

    @Autowired
    public PbftService(Wallet wallet, PbftBlockChain blockChain) {
        this.wallet = wallet;
        this.blockChain = blockChain;
        this.myNode = initMyNode();
        this.totalValidatorMap = initTotalValidator();
        this.isValidator = initValidator();
        this.isActive = false;
        this.isSynced = false;
        if (totalValidatorMap != null) {
            this.bftCount = (totalValidatorMap.size() - 1) / 3;
            this.consenusCount = bftCount * 2 + 1;
        } else {
            this.consenusCount = 0;
            throw new NotValidateException();
        }
    }

    @Override
    public void run(String... args) {
        printInitInfo();
    }

    @Scheduled(cron = "*/5 * * * * *")
    public void mainScheduler() {

        checkNode();

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

            if (pbftStatus.getLastConfirmedBlock().getBlock().getIndex()
                    > this.blockChain.getLastConfirmedBlock().getIndex()) {
                log.debug("this Index: "
                        + this.blockChain.getLastConfirmedBlock().getIndex());
                log.debug("client Index: " + pbftStatus.getLastConfirmedBlock().getIndex());
                log.debug("client : " + client.getId());

                this.isSynced = false;
                blockSyncing(client.getPubKey(),
                        pbftStatus.getLastConfirmedBlock().getIndex());
            } else if (pbftStatus.getLastConfirmedBlock().getIndex()
                    == this.blockChain.getLastConfirmedBlock().getIndex()) {
                // todo: update unconfirm pbftBlock

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
                    log.error("blockConSyncing Verify Fail");
                    continue;
                }
                this.blockChain.getBlockStore().put(pbftBlock.getHash(), pbftBlock);
                this.blockChain.getBlockKeyStore()
                        .put(pbftBlock.getIndex(), pbftBlock.getHash());
            }
            pbftBlock = pbftBlockList.get(i - 1);

            // todo: update unconfirmed pbftMessage

        }

        if (this.blockChain.getLastConfirmedBlock().getIndex() < index) {
            blockSyncing(pubKey, index);
        }
    }


    public PbftStatus getMyNodeStatus() {
        PbftBlock lastConfirmedBlock = this.blockChain.getLastConfirmedBlock();
        PbftBlock unConfirmedBlock = this.blockChain.getUnConfirmedBlock();

        byte[] lastConfirmedBlockHash = new byte[32];
        if (lastConfirmedBlock != null) {
            lastConfirmedBlockHash = lastConfirmedBlock.getHash();
        }

        byte[] unConfirmedBlockHash = new byte[32];
        if (unConfirmedBlock != null) {
            unConfirmedBlockHash = unConfirmedBlock.getHash();
        }

        long timestamp = TimeUtils.time();

        return new PbftStatus(lastConfirmedBlock,
                unConfirmedBlock,
                timestamp,
                wallet.sign(ByteUtil.merge(lastConfirmedBlockHash,
                        unConfirmedBlockHash,
                        ByteUtil.longToBytes(timestamp))));
    }

    private void printInitInfo() {
        log.info("Node Started");
        log.info("wallet address: " + wallet.getHexAddress());
        log.info("wallet pubKey: " + Hex.toHexString(wallet.getPubicKey()));
        log.info("isValidator: " + this.isValidator);
    }

    private Map<String, PbftClientStub> initTotalValidator() {
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
        Map<String, PbftClientStub> nodeMap = new ConcurrentHashMap<>();

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
