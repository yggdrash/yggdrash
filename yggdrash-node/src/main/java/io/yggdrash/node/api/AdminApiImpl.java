package io.yggdrash.node.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.config.DefaultConfig;
import io.yggdrash.core.Wallet;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.util.ByteUtil;
import io.yggdrash.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.restart.RestartEndpoint;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@AutoJsonRpcServiceImpl
public class AdminApiImpl implements AdminApi {

    private static final Logger log = LoggerFactory.getLogger(AdminApiImpl.class);

    private static int COMMAND_ACTIVE_TIME;
    private static int COMMAND_RAND_LENGTH = 8;
    private static int COMMAND_NONCE_LENGTH = 16;

    private String adminMode;
    private String adminIp;
    private byte[] adminPubKey;

    private JsonObject header;
    private String signature;
    private JsonArray body;

    private StringBuilder errorMsg;

    private final ConcurrentHashMap<String, String>
            commandMap = new ConcurrentHashMap<>(); // nonce, timestamp

    private final DefaultConfig defaultConfig;
    private final Wallet wallet;
    private final HttpServletRequest request;
    private final RestartEndpoint restartEndpoint;

    @Autowired
    public AdminApiImpl(DefaultConfig defaultConfig,
                        Wallet wallet,
                        HttpServletRequest request,
                        RestartEndpoint restartEndpoint) {
        this.defaultConfig = defaultConfig;
        this.wallet = wallet;
        this.request = request;
        this.restartEndpoint = restartEndpoint;

        this.adminMode = defaultConfig.getConfig().getString("admin.mode");
        this.adminIp = defaultConfig.getConfig().getString("admin.ip");
        this.adminPubKey = Hex.decode(defaultConfig.getConfig().getString("admin.pubKey"));

        COMMAND_ACTIVE_TIME = defaultConfig.getConfig().getInt("admin.commandTime") * 1000;
    }

    @Override
    public String nodeHello(AdminDto command) {
        // check the adminMode & client ip
        if (!getClientIp().equals(adminIp) || !adminMode.equals("true")) {
            // todo: check the ip fake
            return "Error. IP is not valid.";
        }

        errorMsg = new StringBuilder();

        // check the command validation
        if (!verifyAdminDto(command)) {
            return "Error. " + errorMsg.toString();
        }

        // make a clientHello message
        // create body
        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "clientHello");
        JsonArray body = new JsonArray();
        body.add(bodyObject);

        // create header
        JsonObject header = new JsonObject();

        // - timestamp
        long timestamp = System.currentTimeMillis();
        header.addProperty("timestamp", Hex.toHexString(ByteUtil.longToBytes(timestamp)));

        // - nonce
        byte[] nonce = Hex.decode(this.header.get("nonce").getAsString());
        byte[] newRand = new byte[COMMAND_RAND_LENGTH];
        SecureRandom prng = new SecureRandom();
        prng.nextBytes(newRand);

        byte[] newNonce = new byte[COMMAND_NONCE_LENGTH];
        System.arraycopy(nonce, COMMAND_RAND_LENGTH, newNonce, 0, COMMAND_RAND_LENGTH);
        System.arraycopy(newRand, 0, newNonce, COMMAND_RAND_LENGTH, COMMAND_RAND_LENGTH);
        header.addProperty("nonce", Hex.toHexString(newNonce));

        // - bodyHash
        byte[] bodyHash = HashUtil.sha3(body.toString().getBytes());
        header.addProperty("bodyHash", Hex.toHexString(bodyHash));

        // - bodyLength
        byte[] bodyLength = ByteUtil.longToBytes((long) body.toString().length());
        header.addProperty("bodyLength", Hex.toHexString(bodyLength));

        // create signature
        String signature = Hex.toHexString(wallet.signHashedData(getDataHashForSignHeader(header)));

        JsonObject returnObject = new JsonObject();
        returnObject.add("header", header);
        returnObject.addProperty("signature", signature);
        returnObject.add("body", body);

        this.commandMap.put(Hex.toHexString(newRand),
                Hex.toHexString(ByteUtil.longToBytes(timestamp)));
        // todo: delete the unused data for a long time.

        return returnObject.toString();
    }


    @Override
    public String requestCommand(AdminDto command) {
        // check the adminMode & client ip
        if (!getClientIp().equals(adminIp) || !adminMode.equals("true")) {
            // todo: check the ip fake
            return "Error. IP is not valid.";
        }

        errorMsg = new StringBuilder();

        // check the command validation
        if (!verifyAdminDto(command)) {
            return "Error." + errorMsg.toString();
        }

        // check nonce
        synchronized (commandMap) {
            if (!commandMap.containsKey(header.get("nonce").getAsString()
                    .substring(0, COMMAND_NONCE_LENGTH))) {
                return "Error. Nonce is not valid.";
            }

            commandMap.remove(header.get("nonce").getAsString());
        }

        // execute command
        String methodCommand = body.get(0).getAsJsonObject().get("method").getAsString();

        switch (methodCommand) {
            case "restart":
                // todo: change CLI commander
                restartSpringDaemon();
                break;
            case "setConfig":
                try {

                    // todo: change to config file.
                    String userDir = System.getProperty("user.dir") + "/.yggdrash";
                    File file = new File(userDir, "admin.conf");
                    Set<PosixFilePermission> perms = new HashSet<>();

                    if (file.exists()) {
                        perms.add(PosixFilePermission.OWNER_WRITE);
                        Files.setPosixFilePermissions(file.toPath(), perms);
                    }

                    FileUtil.writeStringToFile(file,
                            body.get(0).getAsJsonObject().get("params").getAsString());

                    perms = new HashSet<>();
                    perms.add(PosixFilePermission.OWNER_READ);
                    perms.add(PosixFilePermission.OWNER_WRITE);
                    Files.setPosixFilePermissions(file.toPath(), perms);

                } catch (Exception e) {
                    log.error(e.getMessage());
                    return "Error. Admin Configuration is not valid.";
                }

                // restart
                // todo: consider CLI restart.
                restartSpringDaemon();

                break;
            default:
                return "Error. Command is not valid.";
        }

        // make a responseCommand message
        // create body
        JsonObject bodyObject = new JsonObject();
        bodyObject.addProperty("method", "responseCommand");
        JsonArray body = new JsonArray();
        body.add(bodyObject);

        // create header
        JsonObject header = new JsonObject();

        // - timestamp
        long timestamp = System.currentTimeMillis();
        header.addProperty("timestamp", Hex.toHexString(ByteUtil.longToBytes(timestamp)));

        // - nonce
        byte[] nonce = Hex.decode(this.header.get("nonce").getAsString());
        byte[] newRand = new byte[COMMAND_RAND_LENGTH];
        SecureRandom prng = new SecureRandom();
        prng.nextBytes(newRand);

        byte[] newNonce = new byte[COMMAND_NONCE_LENGTH];
        System.arraycopy(nonce, COMMAND_RAND_LENGTH, newNonce, 0, COMMAND_RAND_LENGTH);
        System.arraycopy(newRand, 0, newNonce, COMMAND_RAND_LENGTH, COMMAND_RAND_LENGTH);
        header.addProperty("nonce", Hex.toHexString(newNonce));

        // - bodyHash
        byte[] bodyHash = HashUtil.sha3(body.toString().getBytes());
        header.addProperty("bodyHash", Hex.toHexString(bodyHash));

        // - bodyLength
        byte[] bodyLength = ByteUtil.longToBytes((long) body.toString().length());
        header.addProperty("bodyLength", Hex.toHexString(bodyLength));

        // create signature
        String signature = Hex.toHexString(wallet.signHashedData(getDataHashForSignHeader(header)));

        JsonObject returnObject = new JsonObject();
        returnObject.add("header", header);
        returnObject.addProperty("signature", signature);
        returnObject.add("body", body);

        return returnObject.toString();
    }

    private boolean verifyAdminDto(AdminDto command) {

        // null check
        if (command.getHeader() == null || command.getSignature() == null
                || command.getBody() == null) {
            return false;
        }

        this.header = new Gson().fromJson(command.getHeader(), JsonObject.class);
        this.signature = command.getSignature();
        this.body = new Gson().fromJson(command.getBody(), JsonArray.class);

        // body length check
        long bodyLength = ByteUtil.byteArrayToLong(
                Hex.decode(header.get("bodyLength").getAsString()));

        if (body.toString().length() != bodyLength) {
            errorMsg.append(" BodyLength is not valid.");
            return false;
        }

        // timestamp check (3 min)
        long timestamp = ByteUtil.byteArrayToLong(
                Hex.decode(header.get("timestamp").getAsString()));
        if (timestamp < System.currentTimeMillis() - (COMMAND_ACTIVE_TIME)) {
            log.error("Timestamp is not valid.");
            errorMsg.append(" Timestamp is not valid.");
            return false;
        }

        // check bodyHash
        if (!header.get("bodyHash").getAsString().equals(
                Hex.toHexString(HashUtil.sha3(body.toString().getBytes())))) {
            log.error("BodyHash is not valid.");
            errorMsg.append(" BodyHash is not valid.");
            return false;
        }

        // verify a signature
        if (!Wallet.verify(getDataHashForSignHeader(header),
                Hex.decode(signature), true, adminPubKey)) {
            log.error("Signature is not valid.");
            errorMsg.append(" Signature is not valid.");
            return false;
        }

        return true;

    }

    private byte[] getDataHashForSignHeader(JsonObject header) {
        StringBuilder headerValues = new StringBuilder();
        for (String key : header.keySet()) {
            headerValues.append(header.get(key).getAsString());
        }

        return HashUtil.sha3(Hex.decode(headerValues.toString()));
    }

    private String getClientIp() {
        String remoteAddr = "";

        if (request != null) {
            remoteAddr = request.getRemoteAddr();
        }

        return remoteAddr;
    }

    private void restartSpringDaemon() {
        // todo: consider CLI restart.
        Thread restartThread = new Thread(this.restartEndpoint::restart);
        restartThread.setDaemon(false);
        restartThread.start();
    }

}
