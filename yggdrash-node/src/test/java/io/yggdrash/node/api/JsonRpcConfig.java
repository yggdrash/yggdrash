package io.yggdrash.node.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@TestConfiguration
public class JsonRpcConfig {

    private static final Logger log = LoggerFactory.getLogger(JsonRpcConfig.class);

    public BlockApi blockApi() {
        try {
            URL url = new URL("http://localhost:8080/api/block");
            return ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    BlockApi.class, getJsonRpcHttpClient(url));
        } catch (MalformedURLException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public ContractApi contractApi() {
        try {
            URL url = new URL("http://localhost:8080/api/contract");
            return ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    ContractApi.class, getJsonRpcHttpClient(url));
        } catch (MalformedURLException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public ContractApi contractApi(String server) {
        try {
            URL url = new URL("http://" + server + ":8080/api/contract");
            return ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    ContractApi.class, getJsonRpcHttpClient(url));
        } catch (MalformedURLException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public TransactionApi transactionApi() {
        try {
            URL url = new URL("http://localhost:8080/api/transaction");
            return ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    TransactionApi.class, getJsonRpcHttpClient(url));
        } catch (MalformedURLException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public TransactionApi transactionApi(String server) {
        try {
            URL url = new URL("http://" + server + ":8080/api/transaction");
            return ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    TransactionApi.class, getJsonRpcHttpClient(url));
        } catch (MalformedURLException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public AccountApi accountApi() {
        try {
            URL url = new URL("http://localhost:8080/api/account");
            return ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    AccountApi.class, getJsonRpcHttpClient(url));
        } catch (MalformedURLException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public AccountApi accountApi(String server) {
        try {
            URL url = new URL("http://" + server + ":8080/api/account");
            return ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    AccountApi.class, getJsonRpcHttpClient(url));
        } catch (MalformedURLException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public PeerApi peerApi() {
        try {
            URL url = new URL("http://localhost:8080/api/peer");
            return ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    PeerApi.class, getJsonRpcHttpClient(url));
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    private JsonRpcHttpClient getJsonRpcHttpClient(URL endpoint) {
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
}
