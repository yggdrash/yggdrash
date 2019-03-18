package io.yggdrash.node.config;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.pbft.PbftBlockChain;
import io.yggdrash.core.blockchain.pbft.PbftBlockChainBuilder;
import io.yggdrash.core.p2p.Peer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("validator")
@Configuration
public class PbftConfiguration {

    @Bean
    PbftBlockChain pbftBlockChain(Peer owner, BlockChain yggdrash, DefaultConfig defaultConfig) {
        return PbftBlockChainBuilder.Builder()
                .setOwner(owner)
                .setBlockChain(yggdrash)
                .setConfig(defaultConfig)
                .build();
    }
}
