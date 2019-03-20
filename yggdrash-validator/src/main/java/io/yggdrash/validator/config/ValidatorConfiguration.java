package io.yggdrash.validator.config;

import com.typesafe.config.ConfigFactory;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.validator.service.ValidatorService;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class ValidatorConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ValidatorConfiguration.class);

    private final List<ValidatorService> validatorServiceList = new ArrayList<>();

    @Bean
    public void makeValidatorService() throws IOException, InvalidCipherTextException {

        File validatorPath = new File(new DefaultConfig().getString("yggdrash.validator.path"));

        for (File validatorDir : validatorPath.listFiles()) {
            File validatorConfFile = new File(validatorDir, "validator.conf");
            DefaultConfig validatorConfig = new DefaultConfig(ConfigFactory.parseFile(validatorConfFile));
            log.debug(validatorConfig.getString("yggdrash.validator.host"));

            File genesisFile = new File(validatorDir, "genesis.json");
            FileInputStream is = new FileInputStream(genesisFile);
            Block genesisBlock = new Block(
                    JsonUtil.parseJsonObject(
                            IOUtils.toString(is, StandardCharsets.UTF_8)));
            log.debug(genesisBlock.toString());

            validatorServiceList.add(new ValidatorService(validatorConfig, genesisBlock));
        }

    }
}
