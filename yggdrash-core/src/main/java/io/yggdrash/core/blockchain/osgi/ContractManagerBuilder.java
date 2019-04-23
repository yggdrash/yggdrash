package io.yggdrash.core.blockchain.osgi;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.contract.core.store.OutputStore;
import io.yggdrash.contract.core.store.OutputType;
import io.yggdrash.core.blockchain.SystemProperties;
import io.yggdrash.core.store.ContractStore;
import org.osgi.framework.launch.FrameworkFactory;

import java.util.Map;

public class ContractManagerBuilder {
    private FrameworkFactory frameworkFactory;
    private Map<String, String> containerConfig;
    private String branchId;
    private ContractStore contractStore;
    private DefaultConfig config;
    private SystemProperties systemProperties;
    private Map<OutputType, OutputStore> outputStore;

    private ContractManagerBuilder() {

    }

    public static ContractManagerBuilder newInstance() {
        return new ContractManagerBuilder();
    }

    public ContractManagerBuilder withFrameworkFactory(FrameworkFactory frameworkFactory) {
        this.frameworkFactory = frameworkFactory;
        return this;
    }

    public ContractManagerBuilder withContainerConfig(Map<String, String> containerConfig) {
        this.containerConfig = containerConfig;
        return this;
    }

    public ContractManagerBuilder withBranchId(String branchId) {
        this.branchId = branchId;
        return this;
    }

    public ContractManagerBuilder withStoreContainer(ContractStore contractStore) {
        this.contractStore = contractStore;
        return this;

    }

    public ContractManagerBuilder withConfig(DefaultConfig config) {
        this.config = config;
        return this;
    }

    public ContractManagerBuilder withSystemProperties(SystemProperties systemProperties) {
        this.systemProperties = systemProperties;
        return this;
    }

    public ContractManagerBuilder withOutputStore(Map<OutputType, OutputStore> outputStore) {
        this.outputStore = outputStore;
        return this;
    }

    public ContractManager build() {
        if (this.frameworkFactory == null) {
            throw new IllegalStateException("Must set frameworkFactory");
        }

        if (this.containerConfig == null) {
            throw new IllegalStateException("Must set commonContainerConfig");
        }

        if (this.branchId == null) {
            throw new IllegalStateException("Must set branchId");
        }

        ContractManager contractManager = new ContractManager(
                this.frameworkFactory,
                this.containerConfig,
                this.branchId,
                this.contractStore,
                this.config,
                this.systemProperties,
                this.outputStore
        );
        contractManager.newFramework(); //TODO Remove business logic
        return contractManager;
    }

}
