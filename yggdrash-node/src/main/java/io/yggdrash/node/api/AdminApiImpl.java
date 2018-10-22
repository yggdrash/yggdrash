package io.yggdrash.node.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.config.DefaultConfig;
import io.yggdrash.core.Wallet;
import io.yggdrash.crypto.HashUtil;
import io.yggdrash.node.controller.AdminDto;
import io.yggdrash.util.ByteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.restart.RestartEndpoint;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@AutoJsonRpcServiceImpl
public class AdminApiImpl implements AdminApi {

    private static final Logger log = LoggerFactory.getLogger(AdminApiImpl.class);

    private static long COMMAND_ACTIVE_TIME = 60 * 60 * 1000; // 3 min
    // todo: change the time value to config file.

    private static int COMMAND_RAND_LENGTH = 8;
    private static int COMMAND_NONCE_LENGTH = 16;

    private HttpServletRequest request;

    // todo: check the autowired about defaultConfig.
    private DefaultConfig defaultConfig = new DefaultConfig();

    private String adminMode = defaultConfig.getConfig().getString("admin.mode");
    private String adminIp = defaultConfig.getConfig().getString("admin.ip");
    private byte[] adminPubKey = Hex.decode(defaultConfig.getConfig().getString("admin.pubKey"));

    private JsonObject header;
    private String signature;
    private JsonArray body;

    private StringBuilder errorMsg;

    private final ConcurrentHashMap<String, String>
            commandMap = new ConcurrentHashMap<>(); // nonce, timestamp

    @Autowired
    private Wallet wallet;

    @Autowired
    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    @Autowired
    private RestartEndpoint restartEndpoint;

    private String getClientIp() {
        String remoteAddr = "";

        if (request != null) {
            remoteAddr = request.getRemoteAddr();
        }

        return remoteAddr;
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
        byte[] bodyLength = ByteUtil.longToBytes((long)body.toString().length());
        header.addProperty("bodyLength", Hex.toHexString(bodyLength));

        // create signature
        String signature = Hex.toHexString(wallet.sign(header.toString().getBytes()));

        JsonObject returnObject = new JsonObject();
        returnObject.add("header", header);
        returnObject.addProperty("signature", signature);
        returnObject.add("body", body);

        this.commandMap.put(Hex.toHexString(newRand),
                Hex.toHexString(ByteUtil.longToBytes(timestamp)));
        // todo: delete the unused

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
                    .substring(0,COMMAND_NONCE_LENGTH))) {
                return "Error. Nonce is not valid.";
            }

            commandMap.remove(header.get("nonce").getAsString());
        }

        // execute command
        String methodCommand = body.get(0).getAsJsonObject().get("method").getAsString();

        if (methodCommand.equals("restart")) {
            // restart
            // todo: consider CLI restart.
            Thread restartThread = new Thread(() -> restartEndpoint.restart());
            restartThread.setDaemon(false);
            restartThread.start();
        } else if (methodCommand.equals("setConfig")) {
            // setConfig
            Set<Map.Entry<String, JsonElement>> params
                    = body.get(0).getAsJsonObject().get("params").getAsJsonObject().entrySet();

//            for(Map.Entry<String, JsonElement> entry : params) {
//                defaultConfig.
//            }


        } else {
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
        byte[] bodyLength = ByteUtil.longToBytes((long)body.toString().length());
        header.addProperty("bodyLength", Hex.toHexString(bodyLength));

        // create signature
        String signature = Hex.toHexString(wallet.sign(header.toString().getBytes()));

        JsonObject returnObject = new JsonObject();
        returnObject.add("header", header);
        returnObject.addProperty("signature", signature);
        returnObject.add("body", body);

        return returnObject.toString();
    }

    private boolean verifyAdminDto(AdminDto command) {
        //todo: add checking length.

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

        // body message check
//        if (!body.get(0).getAsJsonObject().get("method").getAsString().equals(method)) {
//            errorMsg.append(" Method is not valid.");
//            return false;
//        }

        // timestamp check (3 min)
        long timestamp = ByteUtil.byteArrayToLong(
                Hex.decode(header.get("timestamp").getAsString()));
        if (timestamp < System.currentTimeMillis() - (COMMAND_ACTIVE_TIME)) {
            log.error("Timestamp is not valid.");
            errorMsg.append(" Timestamp is not valid.");
            // return false;
            // todo: delete comment;
        }

        // check bodyHash
        if (!header.get("bodyHash").getAsString().equals(
                Hex.toHexString(HashUtil.sha3(body.toString().getBytes())))) {
            log.error("BodyHash is not valid.");
            errorMsg.append(" BodyHash is not valid.");
            //return false;
        }

        // verify a signature
        if (!Wallet.verify(header.toString().getBytes(),
                Hex.decode(signature), false, adminPubKey)) {
            log.error("Signature is not valid.");
            errorMsg.append(" Signature is not valid.");
            // return false;
        }

        return true;

    }

}
