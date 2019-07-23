package io.yggdrash.core.blockchain.osgi;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.SystemProperties;
import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import io.yggdrash.core.blockchain.osgi.framework.BundleService;
import io.yggdrash.core.blockchain.osgi.framework.FrameworkLauncher;
import io.yggdrash.core.store.ContractStore;
import io.yggdrash.core.store.LogStore;
import org.osgi.framework.launch.FrameworkFactory;

import java.util.Map;

public class ContractManagerBuilder {
    private FrameworkFactory frameworkFactory;
    private Map<String, String> contractManagerConfig;
    private String branchId;
    private ContractStore contractStore;
    private LogStore logStore;

    private SystemProperties systemProperties;

    private String osgiPath;
    private String databasePath;
    private String contractPath;
    private String contractRepositoryUrl;

    private FrameworkLauncher frameworkLauncher;
    private BundleService bundleService;
    private DefaultConfig defaultConfig;
    private GenesisBlock genesis;

    private ContractManagerBuilder() {

    }

    public static ContractManagerBuilder newInstance() {
        return new ContractManagerBuilder();
    }

    public ContractManagerBuilder withFrameworkFactory(FrameworkFactory frameworkFactory) {
        this.frameworkFactory = frameworkFactory;
        return this;
    }

    public ContractManagerBuilder withContractManagerConfig(Map<String, String> contractManagerConfig) {
        this.contractManagerConfig = contractManagerConfig;
        return this;
    }

    public ContractManagerBuilder withBranchId(String branchId) {
        this.branchId = branchId;
        return this;
    }

    public ContractManagerBuilder withContractStore(ContractStore contractStore) {
        this.contractStore = contractStore;
        return this;

    }

    public ContractManagerBuilder withLogStore(LogStore logStore) {
        this.logStore = logStore;
        return this;
    }

    @Deprecated
    public ContractManagerBuilder withConfig(DefaultConfig config) {
        this.osgiPath = config.getOsgiPath();
        this.databasePath = config.getDatabasePath();
        this.contractPath = config.getContractPath();
        return this;
    }

    public ContractManagerBuilder withOsgiPath(String osgiPath) {
        this.osgiPath = osgiPath;
        return this;
    }

    public ContractManagerBuilder withDataBasePath(String databasePath) {
        this.databasePath = databasePath;
        return this;
    }

    public ContractManagerBuilder withContractPath(String contractPath) {
        this.contractPath = contractPath;
        return this;
    }

    public ContractManagerBuilder withSystemProperties(SystemProperties systemProperties) {
        this.systemProperties = systemProperties;
        return this;
    }

    public ContractManagerBuilder withContractRepository(String contractRepositoryUrl) {
        this.contractRepositoryUrl = contractRepositoryUrl;
        return this;
    }

    public ContractManagerBuilder withBundleManager(BundleService bundleService) {
        this.bundleService = bundleService;
        return this;
    }

    public ContractManagerBuilder withBootFramework(FrameworkLauncher bootFramework) {
        this.frameworkLauncher = bootFramework;
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
        if (this.frameworkFactory == null) {
            throw new IllegalStateException("Must set frameworkFactory");
        }

        if (this.contractManagerConfig == null) {
            throw new IllegalStateException("Must set common contractManagerConfigConfig");
        }

        if (this.branchId == null) {
            throw new IllegalStateException("Must set branchId");
        }

        return new ContractManager(
                this.frameworkFactory,
                this.contractManagerConfig,
                this.branchId,
                this.contractStore,
                this.osgiPath,
                this.databasePath,
                this.contractPath,
                this.systemProperties,
                this.logStore,
                this.contractRepositoryUrl,
                this.frameworkLauncher,
                this.bundleService,
                this.defaultConfig,
                this.genesis);
    }

}
