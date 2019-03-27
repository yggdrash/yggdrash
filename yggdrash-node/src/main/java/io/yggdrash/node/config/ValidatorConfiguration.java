package io.yggdrash.node.config;

import com.typesafe.config.ConfigFactory;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.validator.service.ValidatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Profile("validator")
@Configuration
public class ValidatorConfiguration {

    private static final Logger log
            = LoggerFactory.getLogger(io.yggdrash.validator.config.ValidatorConfiguration.class);

    private final Map<BranchId, List<ValidatorService>> validatorServiceMap = new ConcurrentHashMap<>();

    @Bean
    public Map<BranchId, List<ValidatorService>> makeValidatorService(BranchGroup branchGroup)
            throws IOException, InvalidCipherTextException {
        File validatorPath = new File(new DefaultConfig().getString("yggdrash.validator.path"));

        for (File branchPath : Objects.requireNonNull(validatorPath.listFiles())) {
            Block genesisBlock = null;
            String branchPathName = branchPath.getName();
            if (branchPathName.length() != Constants.BRANCH_HEX_LENGTH
                    || !branchPathName.matches("^[0-9a-fA-F]+$")) {
                continue;
            }

            BranchId branchId = new BranchId(new Sha3Hash(branchPath.getName()));
            try {
                genesisBlock = branchGroup.getBranch(branchId).getGenesisBlock().getCoreBlock();
            } finally {
                if (genesisBlock == null) {
                    continue;
                }
            }
            log.debug(genesisBlock.toString());

            List<ValidatorService> validatorServiceList = new ArrayList<>();
            for (File validatorServicePath : Objects.requireNonNull(branchPath.listFiles())) {
                File validatorConfFile = new File(validatorServicePath, "validator.conf");
                DefaultConfig validatorConfig
                        = new DefaultConfig(ConfigFactory.parseFile(validatorConfFile));
                log.debug(validatorConfig.getString("yggdrash.validator.host"));
                validatorServiceList.add(new ValidatorService(validatorConfig, genesisBlock));
            }
            validatorServiceMap.put(branchId, validatorServiceList);
        }
        return validatorServiceMap;
    }
}
