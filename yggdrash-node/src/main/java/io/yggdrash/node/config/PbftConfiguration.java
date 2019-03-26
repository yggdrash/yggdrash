package io.yggdrash.node.config;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.pbft.PbftBlockChain;
import io.yggdrash.core.blockchain.pbft.PbftBlockChainBuilder;
import io.yggdrash.core.net.ValidatorBlockBroadcaster;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.core.p2p.PeerDialer;
import io.yggdrash.core.p2p.PeerHandlerFactory;
import io.yggdrash.core.p2p.SimplePeerDialer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.stream.Collectors;

@Profile("validator")
@Configuration
public class PbftConfiguration {

    private final NodeProperties nodeProperties;

    PbftConfiguration(NodeProperties nodeProperties) {
        this.nodeProperties = nodeProperties;
    }

    @Bean
    PbftBlockChain pbftBlockChain(Peer owner, BlockChain yggdrash, DefaultConfig defaultConfig) {
        return PbftBlockChainBuilder.newBuilder()
                .setOwner(owner)
                .setBlockChain(yggdrash)
                .setConfig(defaultConfig)
                .build();
    }

    @Bean
    ValidatorBlockBroadcaster validatorBlockBroadcaster(BlockChain yggdrash, PeerHandlerFactory peerHandlerFactory) {
        PeerDialer peerDialer = new SimplePeerDialer(peerHandlerFactory);
        List<Peer> broadcastPeerList = nodeProperties.getBroadcastPeerList().stream()
                .map(Peer::valueOf).collect(Collectors.toList());
        ValidatorBlockBroadcaster broadcaster = new ValidatorBlockBroadcaster(peerDialer, broadcastPeerList);
        yggdrash.addListener(broadcaster);
        return broadcaster;
    }
}
