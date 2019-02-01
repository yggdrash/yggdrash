package io.yggdrash.validator.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.data.BlockConChain;
import io.yggdrash.validator.data.PbftBlockChain;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class NodeConfiguration {

    @Bean
    DefaultConfig defaultConfig() {
        return new DefaultConfig();
    }

    @Bean
    Wallet wallet(DefaultConfig defaultConfig) throws IOException, InvalidCipherTextException {
        return new Wallet(defaultConfig);
    }

    @Bean
    Block genesisBlock() {
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
    BlockConChain blockConChain(Block genesisBlock, DefaultConfig defaultConfig) {
        return new BlockConChain(genesisBlock, defaultConfig);
    }

    @Bean
    @ConditionalOnProperty(name = "yggdrash.validator.consensus.algorithm", havingValue = "pbft")
    PbftBlockChain pbftBlockChain(Block genesisBlock, DefaultConfig defaultConfig) {
        return new PbftBlockChain(genesisBlock, defaultConfig);
    }

}
