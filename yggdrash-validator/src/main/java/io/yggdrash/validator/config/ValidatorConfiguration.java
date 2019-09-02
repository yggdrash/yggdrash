package io.yggdrash.validator.config;

import com.typesafe.config.ConfigFactory;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.exception.FailedOperationException;
import io.yggdrash.common.utils.FileUtil;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BlockImpl;
import io.yggdrash.validator.service.ValidatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static io.yggdrash.common.config.Constants.VALIDATOR_PATH;

@Configuration
public class ValidatorConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ValidatorConfiguration.class);

    private final List<ValidatorService> validatorServiceList = new ArrayList<>();

    @Bean
    public void makeValidatorService() {
        File validatorPath = new File(new DefaultConfig().getString(VALIDATOR_PATH));
        if (!validatorPath.exists() || validatorPath.listFiles() == null) {
            throw new FailedOperationException("Can't read validatorPath=" + validatorPath.getAbsolutePath());
        }
        try {
            for (File validatorDir : validatorPath.listFiles()) {
                if (validatorDir == null) {
                    continue;
                }
                File validatorConfFile = new File(validatorDir, "validator.conf");
                DefaultConfig validatorConfig = new DefaultConfig(ConfigFactory.parseFile(validatorConfFile));
                log.debug(validatorConfig.getString("yggdrash.validator.host"));

                File genesisFile = new File(validatorDir, "genesis.json");
                String genesisString = FileUtil.readFileToString(genesisFile, FileUtil.DEFAULT_CHARSET);
                Block genesisBlock = new BlockImpl(JsonUtil.parseJsonObject(genesisString));
                log.debug("{}", genesisBlock.getBranchId());

                validatorServiceList.add(new ValidatorService(validatorConfig, genesisBlock));
            }
        } catch (Exception e) {
            log.error("Validator configuration is not valid. {}", validatorPath.getAbsolutePath());
            log.debug(e.getMessage());
            System.exit(-1);
        }
    }
}
