package io.yggdrash.core.blockchain.osgi;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.contract.core.store.OutputStore;
import io.yggdrash.contract.core.store.OutputType;
import io.yggdrash.core.blockchain.SystemProperties;
import io.yggdrash.core.store.StoreContainer;
import io.yggdrash.core.store.TransactionReceiptStore;
import org.osgi.framework.launch.FrameworkFactory;
import java.util.Map;

public class ContractContainerBuilder {
    private FrameworkFactory frameworkFactory;
    private Map<String, String> containerConfig;
    private String branchId;
    private StateStore stateStore;
    private StoreContainer storeContainer;
    private TransactionReceiptStore transactionReceiptStore;
    private DefaultConfig config;
    private SystemProperties systemProperties;
    private Map<OutputType, OutputStore> outputStore;

    private ContractContainerBuilder() {

    }

    public static ContractContainerBuilder newInstance() {
        return new ContractContainerBuilder();
    }

    public ContractContainerBuilder withFrameworkFactory(FrameworkFactory frameworkFactory) {
        this.frameworkFactory = frameworkFactory;
        return this;
    }

    public ContractContainerBuilder withContainerConfig(Map<String, String> containerConfig) {
        this.containerConfig = containerConfig;
        return this;
    }

    public ContractContainerBuilder withBranchId(String branchId) {
        this.branchId = branchId;
        return this;
    }

    public ContractContainerBuilder withStoreContainer(StoreContainer storeContainer) {
        this.storeContainer = storeContainer;
        return this;

    }

    public ContractContainerBuilder withConfig(DefaultConfig config) {
        this.config = config;
        return this;
    }

    public ContractContainerBuilder withSystemProperties(SystemProperties systemProperties) {
        this.systemProperties = systemProperties;
        return this;
    }

    public ContractContainerBuilder withOutputStore(Map<OutputType, OutputStore> outputStore) {
        this.outputStore = outputStore;
        return this;
    }

    public ContractContainer build() {
        if (this.frameworkFactory == null) {
            throw new IllegalStateException("Must set frameworkFactory");
        }

        if (this.containerConfig == null) {
            throw new IllegalStateException("Must set commonContainerConfig");
        }

        if (this.branchId == null) {
            throw new IllegalStateException("Must set branchId");
        }

        ContractContainer contractContainer = new ContractContainer(
                this.frameworkFactory,
                this.containerConfig,
                this.branchId,
                this.storeContainer,
                this.config,
                this.systemProperties,
                this.outputStore
        );
        contractContainer.newFramework();
        return contractContainer;
    }
}
