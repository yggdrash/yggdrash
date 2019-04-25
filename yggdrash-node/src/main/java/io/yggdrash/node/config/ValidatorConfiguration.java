package io.yggdrash.node.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.node.service.ValidatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Profile("validator")
@Configuration
public class ValidatorConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ValidatorConfiguration.class);

    @Bean
    public Map<BranchId, List<ValidatorService>> validatorServiceMap(
            BranchGroup branchGroup, DefaultConfig defaultConfig) {

        Map<BranchId, List<ValidatorService>> validatorServiceMap = new HashMap<>();
        File validatorPath = new File(defaultConfig.getValidatorPath());

        for (File branchPath : Objects.requireNonNull(validatorPath.listFiles())) {

            BranchId branchId = parseBranchId(branchPath);
            BlockChain branch = branchGroup.getBranch(branchId);
            if (branch == null) {
                log.warn("Not found branch for [{}]", branchPath);
                continue;
            }
            List<ValidatorService> validatorServiceList = loadValidatorService(branchPath, branch, defaultConfig);
            validatorServiceMap.put(branchId, validatorServiceList);
        }

        return validatorServiceMap;
    }

    private BranchId parseBranchId(File branchPath) {

        String branchPathName = branchPath.getName();
        if (branchPathName.length() != Constants.BRANCH_HEX_LENGTH
                || !branchPathName.matches("^[0-9a-fA-F]+$")) {
            return null;
        }

        return new BranchId(new Sha3Hash(branchPath.getName()));
    }

    private List<ValidatorService> loadValidatorService(File branchPath, BlockChain branch,
                                                        DefaultConfig defaultConfig) {

        List<ValidatorService> validatorServiceList = new ArrayList<>();
        for (File validatorServicePath : Objects.requireNonNull(branchPath.listFiles())) {
            File validatorConfFile = new File(validatorServicePath, "validator.conf");
            Config referenceConfig = ConfigFactory.parseFile(validatorConfFile);
            Config config = defaultConfig.getConfig().withFallback(referenceConfig);
            DefaultConfig validatorConfig = new DefaultConfig(config, defaultConfig.isProductionMode());
            log.debug("{}:{}, key={}", validatorConfig.getString("yggdrash.validator.host"),
                    validatorConfig.getString("yggdrash.validator.port"),
                    validatorConfig.getString("yggdrash.validator.key.path"));
            try {
                validatorServiceList.add(new ValidatorService(validatorConfig, branch));
            } catch (Exception e) {
                log.warn(e.getMessage());
            }
        }

        return validatorServiceList;
    }
}
