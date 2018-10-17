package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.config.Constants;
import io.yggdrash.config.DefaultConfig;
import io.yggdrash.node.controller.AdminDto;
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
            if (remoteAddr == null || "".equals(remoteAddr)) {
                remoteAddr = request.getRemoteAddr();
            }
        }

        return remoteAddr;
    }

    @Override
    public String nodeHello(AdminDto command) {

        // get admin ip
        // check the adminMode & client ip
        if(!getClientIp().equals(adminIp) || !adminMode.equals("true")) {
            // todo: check the ip fake
            return null;
        }

        // check the command validation

        // make a clientHello message

        return "OK";
    }




}
