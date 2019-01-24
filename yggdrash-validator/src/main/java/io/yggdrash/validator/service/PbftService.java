package io.yggdrash.validator.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.data.PbftBlockChain;
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
    private final int consenusCount;

    private final Wallet wallet;
    private final PbftBlockChain pbftBlockChain;

    private final PbftClientStub myNode;
    private final Map<String, PbftClientStub> totalValidatorMap;

    private boolean isActive;
    private boolean isSynced;

    private ReentrantLock lock = new ReentrantLock();

    @Autowired
    public PbftService(Wallet wallet, PbftBlockChain pbftBlockChain) {
        this.wallet = wallet;
        this.pbftBlockChain = pbftBlockChain;
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

    @Scheduled(cron = "*/10 * * * * *")
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
                log.info("Node is activated. Start make a proposed Block.");
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
