package io.yggdrash.validator.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.data.BlockConChain;
import org.spongycastle.crypto.InvalidCipherTextException;
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
    BlockHusk genesisBlock() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();

        String genesisString;
        ClassPathResource cpr = new ClassPathResource("genesis/genesis.json");
        try {
            byte[] bdata = FileCopyUtils.copyToByteArray(cpr.getInputStream());
            genesisString = new String(bdata, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IOException("Error genesisFile");
        }

        return new BlockHusk(
                new Block(new Gson().fromJson(genesisString,
                        JsonObject.class)).toProtoBlock());
    }

    @Bean
    BlockConChain blockConChain(BlockHusk genesisBlock) {
        return new BlockConChain(genesisBlock);
    }

}
