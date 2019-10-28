package io.yggdrash.core.blockchain.osgi;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.SystemProperties;
import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import io.yggdrash.core.blockchain.osgi.framework.BundleService;
import io.yggdrash.core.store.ContractStore;
import io.yggdrash.core.store.LogStore;

public class ContractManagerBuilder {
    private ContractStore contractStore;
    private LogStore logStore;

    private SystemProperties systemProperties;

    private BundleService bundleService;
    private DefaultConfig defaultConfig;
    private GenesisBlock genesis;

    private ContractManagerBuilder() {

    }

    public static ContractManagerBuilder newInstance() {
        return new ContractManagerBuilder();
    }

    public ContractManagerBuilder withContractStore(ContractStore contractStore) {
        this.contractStore = contractStore;
        return this;
    }

    public ContractManagerBuilder withLogStore(LogStore logStore) {
        this.logStore = logStore;
        return this;
    }


    public ContractManagerBuilder withSystemProperties(SystemProperties systemProperties) {
        this.systemProperties = systemProperties;
        return this;
    }

    public ContractManagerBuilder withBundleManager(BundleService bundleService) {
        this.bundleService = bundleService;
        return this;
    }

    public ContractManagerBuilder withDefaultConfig(DefaultConfig defaultConfig) {
        this.defaultConfig = defaultConfig;
        return this;
    }

    public ContractManagerBuilder withGenesis(GenesisBlock genesis) {
        this.genesis = genesis;
        return this;
    }

    public ContractManager build() {
        if (this.genesis == null) {
            throw new IllegalStateException("Must set genesis");
        }

        return new ContractManager(
                this.genesis,
                this.bundleService,
                this.defaultConfig,
                this.contractStore,
                this.logStore,
                this.systemProperties
                );
    }

}
