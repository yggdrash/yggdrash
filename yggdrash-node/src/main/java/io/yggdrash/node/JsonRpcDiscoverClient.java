package io.yggdrash.node;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;
import io.yggdrash.core.BranchId;
import io.yggdrash.core.exception.FailedOperationException;
import io.yggdrash.core.net.DiscoveryClient;
import io.yggdrash.core.net.Peer;
import io.yggdrash.node.api.PeerApi;
import io.yggdrash.node.api.PeerDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JsonRpcDiscoverClient implements DiscoveryClient {

    private static final Logger log = LoggerFactory.getLogger(JsonRpcDiscoverClient.class);

    @Override
    public List<String> findPeers(String host, int port, Peer owner) {
        PeerApi peerApi = peerApi(host, port);
        return Optional.ofNullable(owner)
                .map(o -> PeerDto.valueOf(BranchId.STEM, o))
                .map(peerApi::getPeers)
                .orElse(new ArrayList<>());
    }

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

    private PeerApi peerApi(String host, int port) {
        try {
            String spec = "http://" + host + ":" + port;
            URL url = new URL(spec);
            return ProxyUtil.createClientProxy(getClass().getClassLoader(),
                    PeerApi.class, jsonRpcHttpClient(url));
        } catch (Exception e) {
            throw new FailedOperationException(e);
        }
    }
}
