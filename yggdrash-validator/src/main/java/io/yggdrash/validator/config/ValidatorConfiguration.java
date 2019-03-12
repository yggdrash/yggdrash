package io.yggdrash.validator.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.data.ebft.EbftBlockChain;
import io.yggdrash.validator.data.pbft.PbftBlockChain;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import static io.yggdrash.common.config.Constants.DEFAULT_PORT;

@Configuration
public class ValidatorConfiguration {

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
    DefaultConfig defaultConfig() {
        return new DefaultConfig();
    }

    @Bean
    Wallet wallet(DefaultConfig defaultConfig) throws IOException, InvalidCipherTextException {
        return new Wallet(defaultConfig);
    }

    @Bean
    @Qualifier("validatorGenesisBlock")
    Block validatorGenesisBlock() {
        String genesisString;
        ClassPathResource cpr = new ClassPathResource("genesis/genesis.json");
        try {
            byte[] bdata = FileCopyUtils.copyToByteArray(cpr.getInputStream());
            genesisString = new String(bdata, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new NotValidateException("Error genesisFile");
        }

        return new Block(new Gson().fromJson(genesisString, JsonObject.class));
    }

    @Bean
    @ConditionalOnProperty(name = "yggdrash.validator.consensus.algorithm", havingValue = "ebft")
    EbftBlockChain ebftBlockChain(@Qualifier("validatorGenesisBlock") Block genesisBlock, DefaultConfig defaultConfig) {
        return new EbftBlockChain(genesisBlock, defaultConfig);
    }

    @Bean
    @DependsOn( {"validatorGenesisBlock"})
    PbftBlockChain pbftBlockChain(@Qualifier("validatorGenesisBlock") Block genesisBlock, DefaultConfig defaultConfig) {
        String dbPath = defaultConfig.getDatabasePath();
        String keyStorePath = grpcHost() + "_" + grpcPort() + "/"
                + Hex.toHexString(genesisBlock.getHeader().getChain()) + "/pbftKey";
        String blockStorePath = grpcHost() + "_" + grpcPort() + "/"
                + Hex.toHexString(genesisBlock.getHeader().getChain()) + "/pbftBlock";
        String txStorePath = grpcHost() + "_" + grpcPort() + "/"
                + Hex.toHexString(genesisBlock.getHeader().getChain()) + "/pbftTx";

        return new PbftBlockChain(genesisBlock, dbPath, keyStorePath, blockStorePath,
                txStorePath);
    }
}
