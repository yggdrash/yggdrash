package io.yggdrash.node.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.config.Constants;
import io.yggdrash.config.DefaultConfig;
import io.yggdrash.core.Wallet;
import io.yggdrash.node.controller.AdminDto;
import io.yggdrash.util.ByteUtil;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

@Service
@AutoJsonRpcServiceImpl
public class AdminApiImpl implements AdminApi {

    private HttpServletRequest request;
    private DefaultConfig defaultConfig = new DefaultConfig();
    private String adminMode = defaultConfig.getConfig().getString("admin.mode");
    private String adminIp = defaultConfig.getConfig().getString("admin.ip");

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
        // get admin ip
        // check the adminMode & client ip
        if (!getClientIp().equals(adminIp) || !adminMode.equals("true")) {
            // todo: check the ip fake
            return "Error";
        }

        // check the command validation
        if (!verifyAdminDto(command, "nodeHello")) {
            return "Error";
        }


        // make a clientHello message

        return "OK";
    }

    private boolean verifyAdminDto(AdminDto command, String method) {
        // null check
        if (command.getHeader() == null || command.getSignature() == null
                || command.getBody() == null) {
            return false;
        }

        JsonObject header = new JsonParser().parse(command.getHeader()).getAsJsonObject();
        String signature = command.getSignature();
        JsonArray body = new JsonParser().parse(command.getBody()).getAsJsonArray();

        // body length check
        long bodyLength = ByteUtil.byteArrayToLong(
                Hex.decode(header.get("bodyLength").getAsString()));

        if (command.getBody().length() != bodyLength) {
            return false;
        }

        // body message check
        if (!body.get(0).getAsJsonObject().get("method").getAsString().equals(method)) {
            return false;
        }

        // timestamp check (3 min)
        long timestamp = ByteUtil.byteArrayToLong(
                Hex.decode(header.get("timestamp").getAsString()));
        if (timestamp < System.currentTimeMillis() - (3 * 60 * 1000)) {
            return false;
        }

        // check bodyHash

        // verify a signature

        return true;

    }




}
