package io.yggdrash.node.config;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.node.service.pbft.PbftBlockChain;
import org.spongycastle.util.encoders.Hex;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;

import java.net.InetAddress;

import static io.yggdrash.common.config.Constants.DEFAULT_PORT;

@Profile("validator")
@Configuration
public class PbftConfiguration {

    @Bean
    String grpcHost() {
        return InetAddress.getLoopbackAddress().getHostAddress();
    }

    @Bean
    long grpcPort() {
        if (System.getProperty("grpc.port") == null) {
            return DEFAULT_PORT;
        } else {
            return Integer.parseInt(System.getProperty("grpc.port"));
        }
    }

    @Bean
    @DependsOn( {"yggdrash", "branchGroup", "branchLoader"})
    Block genesisBlock(BranchGroup branchGroup) {
        return (((BlockChain) branchGroup.getAllBranch().toArray()[0])
                .getBlockByIndex(0)).getCoreBlock();
    }

    @Bean
    @DependsOn( {"yggdrash", "branchGroup", "branchLoader"})
    PbftBlockChain pbftBlockChain(Block genesisBlock, DefaultConfig defaultConfig) {
        String dbPath = defaultConfig.getDatabasePath();
        String keyStorePath = grpcHost() + "_" + grpcPort() + "/"
                + Hex.toHexString(genesisBlock.getHeader().getChain()) + "/nodePbftKey";
        String blockStorePath = grpcHost() + "_" + grpcPort() + "/"
                + Hex.toHexString(genesisBlock.getHeader().getChain()) + "/nodePbftBlock";
        String txStorePath = grpcHost() + "_" + grpcPort() + "/"
                + Hex.toHexString(genesisBlock.getHeader().getChain()) + "/nodePbftTx";

        return new PbftBlockChain(genesisBlock, dbPath, keyStorePath, blockStorePath,
                txStorePath);
    }

}
