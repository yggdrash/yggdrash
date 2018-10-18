package io.yggdrash.node.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.security.SecureRandom;

@Service
@AutoJsonRpcServiceImpl
public class AdminApiImpl implements AdminApi {

    private static final Logger log = LoggerFactory.getLogger(AdminApiImpl.class);

    private static long COMMAND_ACTIVE_TIME = 3 * 60 * 1000;

    private HttpServletRequest request;
    private DefaultConfig defaultConfig = new DefaultConfig();
    private String adminMode = defaultConfig.getConfig().getString("admin.mode");
    private String adminIp = defaultConfig.getConfig().getString("admin.ip");

    private JsonObject header;
    private String signature;
    private JsonArray body;

    private StringBuilder errorMsg;

    @Autowired
    private Wallet wallet;

    @Autowired
    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

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
            return "Error " + " IP is not valid.";
        }

        errorMsg = new StringBuilder();

        // check the command validation
        if (!verifyAdminDto(command, "nodeHello")) {
            return "Error" + errorMsg.toString();
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
        byte[] newRand = new byte[8];
        SecureRandom prng = new SecureRandom();
        prng.nextBytes(newRand);

        byte[] newNonce = new byte[16];
        System.arraycopy(nonce, 8, newNonce, 0, 8);
        System.arraycopy(newRand, 0, newNonce, 8, 8);
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

    private boolean verifyAdminDto(AdminDto command, String method) {
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
        if (!body.get(0).getAsJsonObject().get("method").getAsString().equals(method)) {
            errorMsg.append(" Method is not valid.");
            return false;
        }

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
            errorMsg.append("\nBodyHash is not valid.");
            return false;
        }

        // verify a signature
        if (!wallet.verify(header.toString().getBytes(), Hex.decode(signature))) {
            log.error("Signature is not valid.");
            errorMsg.append(" Signature is not valid.");
            return false;
        }

        return true;

    }

}
