package io.yggdrash.core.net;

import java.util.List;

public interface DiscoveryClient {

    List<String> findPeers(String host, int port, Peer peer);
}
