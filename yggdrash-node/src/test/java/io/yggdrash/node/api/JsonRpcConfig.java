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

    static final NodeApi NODE_API = new JsonRpcConfig().proxyOf(NodeApi.class);
    static final BlockApi BLOCK_API = new JsonRpcConfig().proxyOf(BlockApi.class);
    static final BranchApi BRANCH_API = new JsonRpcConfig().proxyOf(BranchApi.class);
    static final TransactionApi TX_API = new JsonRpcConfig().proxyOf(TransactionApi.class);
    static final ContractApi CONTRACT_API = new JsonRpcConfig().proxyOf(ContractApi.class);
    static final PeerApi PEER_API = new JsonRpcConfig().proxyOf(PeerApi.class);
    static final LogApi LOG_API = new JsonRpcConfig().proxyOf(LogApi.class);

    private <T> T proxyOf(Class<T> proxyInterface) {
        return proxyOf("localhost", proxyInterface);
    }

    public <T> T proxyOf(String server, Class<T> proxyInterface) {
        try {
            String apiPath = proxyInterface.getSimpleName().toLowerCase().replace("api", "");
            URL url = null;
            if (server.indexOf("http://") > -1) {
                // Server is api url
                url = new URL(String.format("%s/%s",server.trim(),apiPath));
            } else {
                // Server is just ip address
                url = new URL(String.format("http://%s:8080/api/%s", server, apiPath));
            }


            return ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    proxyInterface, getJsonRpcHttpClient(url));
        } catch (MalformedURLException exception) {
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
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true);
        return new JsonRpcHttpClient(objectMapper, url, map);
    }
}
