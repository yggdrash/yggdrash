package io.yggdrash.node.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@TestConfiguration
public class JsonRpcConfig {

    private static final Logger log = LoggerFactory.getLogger(JsonRpcConfig.class);

    //    private static final String endpoint = "http://localhost:8080/api/transaction";
    //
    //    @Bean
    //    public JsonRpcHttpClient jsonRpcHttpClient() {
    //        URL url = null;
    //        //You can add authentication headers etc to this map
    //        Map<String, String> map = new HashMap<>();
    //        try {
    //            url = new URL(JsonRpcConfig.endpoint);
    //        } catch (Exception e) {
    //            log.debug(e.getMessage());
    //        }
    //        ObjectMapper objectMapper = new ObjectMapper();
    //        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    //        return new JsonRpcHttpClient(objectMapper, url, map);
    //    }

    private JsonRpcHttpClient jsonRpcHttpClient(URL endpoint) {
        URL url = null;
        Map<String, String> map = new HashMap<>();
        try {
            url = endpoint;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return new JsonRpcHttpClient(objectMapper, url, map);
    }

    public BlockApi blockApi() {
        try {
            URL url = new URL("http://localhost:8080/api/block");
            return ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    BlockApi.class, jsonRpcHttpClient(url));
        } catch (MalformedURLException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public TransactionApi transactionApi() {
        try {
            URL url = new URL("http://localhost:8080/api/transaction");
            return ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    TransactionApi.class, jsonRpcHttpClient(url));
        } catch (MalformedURLException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public AccountApi accountApi() {
        try {
            URL url = new URL("http://localhost:8080/api/account");
            return ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    AccountApi.class, jsonRpcHttpClient(url));
        } catch (MalformedURLException exception) {
            exception.printStackTrace();
            return null;
        }
    }
}
