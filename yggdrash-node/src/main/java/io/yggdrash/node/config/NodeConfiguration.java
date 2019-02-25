package io.yggdrash.node.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.node.service.pbft.PbftBlockChain;
import org.apache.commons.io.IOUtils;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import static io.yggdrash.common.config.Constants.DEFAULT_PORT;

@Configuration
public class NodeConfiguration {

    @Value("classpath:/genesis.json")
    Resource genesisResource;

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
    Block genesisBlock() {
        String genesisString;
        try {
            InputStream bdata = genesisResource.getInputStream();
            genesisString = IOUtils.toString(bdata, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new NotValidateException("Error genesisFile");
        }

        return new Block(new Gson().fromJson(genesisString, JsonObject.class));
    }

    @Bean
    PbftBlockChain pbftBlockChain(Block genesisBlock, DefaultConfig defaultConfig) {
        String dbPath = defaultConfig.getDatabasePath();
        String keyStorePath = grpcHost() + "_" + grpcPort() + "/"
                + Hex.toHexString(genesisBlock.getHeader().getChain()) + "/pbftKey";
        String blockStorePath = grpcHost() + "_" + grpcPort() + "/"
                + Hex.toHexString(genesisBlock.getHeader().getChain()) + "/pbftBlock";
        String txStorePath = grpcHost() + "_" + grpcPort() + "/"
                + Hex.toHexString(genesisBlock.getHeader().getChain()) + "/pbftTx";

        return new PbftBlockChain(genesisBlock, defaultConfig, dbPath, keyStorePath, blockStorePath,
                txStorePath);
    }

}
