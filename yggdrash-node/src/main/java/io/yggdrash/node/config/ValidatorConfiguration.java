package io.yggdrash.node.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.config.Constants.ActiveProfiles;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.SystemProperties;
import io.yggdrash.core.blockchain.genesis.BranchLoader;
import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import io.yggdrash.core.blockchain.osgi.ContractPolicyLoader;
import io.yggdrash.core.consensus.Consensus;
import io.yggdrash.core.store.BlockChainStore;
import io.yggdrash.core.store.BlockChainStoreBuilder;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.node.service.ValidatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Profile(ActiveProfiles.VALIDATOR)
@Configuration
public class ValidatorConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ValidatorConfiguration.class);

    @SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
    @Autowired(required = false)
    SystemProperties systemProperties;

    @Bean
    public Map<BranchId, List<ValidatorService>> validatorServiceMap(BranchLoader branchLoader,
                                                                     DefaultConfig defaultConfig,
                                                                     ContractPolicyLoader policyLoader) {

        Map<BranchId, List<ValidatorService>> validatorServiceMap = new HashMap<>();
        File validatorPath = new File(defaultConfig.getValidatorPath());

        for (File branchPath : Objects.requireNonNull(validatorPath.listFiles())) {
            Optional<GenesisBlock> genesisBlock = getGenesisBlock(branchPath, branchLoader);
            if (!genesisBlock.isPresent()) {
                log.warn("Not found branch for [{}]", branchPath);
                continue;
            }
            GenesisBlock genesis = genesisBlock.get();
            List<ValidatorService> validatorServiceList =
                    loadValidatorService(branchPath, genesis, genesis.getConsensus(), defaultConfig, policyLoader);
            validatorServiceMap.put(genesis.getBranchId(), validatorServiceList);
        }

        return validatorServiceMap;
    }

    private Optional<GenesisBlock> getGenesisBlock(File branchPath, BranchLoader branchLoader) {

        String branchName = branchPath.getName();
        if (branchName.length() != Constants.BRANCH_HEX_LENGTH || !branchName.matches("^[0-9a-fA-F]+$")) {
            return Optional.empty();
        }

        BranchId branchId = new BranchId(new Sha3Hash(branchPath.getName()));

        return branchLoader.getGenesisBlock(branchId);
    }

    private List<ValidatorService> loadValidatorService(File branchPath, GenesisBlock genesis, Consensus consensus,
                                                        DefaultConfig defaultConfig,
                                                        ContractPolicyLoader policyLoader) {

        List<ValidatorService> validatorServiceList = new ArrayList<>();
        for (File validatorServicePath : Objects.requireNonNull(branchPath.listFiles())) {
            File validatorConfFile = new File(validatorServicePath, "validator.conf");
            if (!validatorConfFile.exists()) {
                continue;
            }

            Config referenceConfig = ConfigFactory.parseFile(validatorConfFile);
            Config config = referenceConfig.withFallback(defaultConfig.getConfig()).resolve();
            DefaultConfig validatorConfig = new DefaultConfig(config, defaultConfig.isProductionMode());
            log.debug("{}:{}, key={}", validatorConfig.getString("yggdrash.validator.host"),
                    validatorConfig.getString("yggdrash.validator.port"),
                    validatorConfig.getString("yggdrash.validator.key.path"));
            try {
                BranchId branchId = genesis.getBranch().getBranchId();

                BlockChainStoreBuilder builder = BlockChainStoreBuilder.newBuilder(branchId);
                builder.withConfig(defaultConfig)
                        .setBlockStoreFactory(ValidatorService.blockStoreFactory())
                        .setConsensusAlgorithm(consensus.getAlgorithm())
                ;
                BlockChainStore blockChainStore = builder.build();

                BlockChain blockChain = BranchConfiguration.getBlockChain(defaultConfig,
                        genesis, blockChainStore, policyLoader,
                        branchId, systemProperties);

                validatorServiceList.add(new ValidatorService(validatorConfig, blockChain));
            } catch (Exception e) {
                log.warn("Load validatorService conf={}, err={}", validatorServicePath, e.getMessage());
            }
        }

        return validatorServiceList;
    }
}
