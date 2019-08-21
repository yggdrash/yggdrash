package io.yggdrash.core.blockchain.osgi;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.SystemProperties;
import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import io.yggdrash.core.blockchain.osgi.framework.BootFrameworkConfig;
import io.yggdrash.core.blockchain.osgi.framework.BootFrameworkLauncher;
import io.yggdrash.core.blockchain.osgi.framework.BundleService;
import io.yggdrash.core.blockchain.osgi.framework.BundleServiceImpl;
import io.yggdrash.core.blockchain.osgi.framework.FrameworkConfig;
import io.yggdrash.core.blockchain.osgi.framework.FrameworkLauncher;
import io.yggdrash.core.store.BlockChainStore;
import io.yggdrash.core.store.BlockChainStoreBuilder;
import io.yggdrash.core.store.ContractStore;
import io.yggdrash.core.store.PbftBlockStoreMock;
import org.apache.commons.codec.binary.Hex;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public class ContractManagerTest {
    private static final Logger log = LoggerFactory.getLogger(ContractManagerBuilderTest.class);

    private static ContractManager manager;
    private static DefaultConfig config;
    private static GenesisBlock genesis;
    private static SystemProperties systemProperties;
    private static BranchId branchId;
    private static BlockChainStore bcStore;
    private static ContractStore contractStore;

    private static GenesisBlock coinGenesis;
    private static BranchId coinBranchId;

    private static ContractExecutor executor;

    private static ContractVersion coinContract = ContractVersion.of("8c65bc05e107aab9ceaa872bbbb2d96d57811de4");
    ContractVersion notExistedVersion = ContractVersion.of(Hex.encodeHexString("Wrong ContractVersion".getBytes()));

    @Before
    public void setUp() throws Exception {
        manager = initContractManager();
        executor = manager.getContractExecutor();

    }

    private void printBundles() {
        Bundle[] bundles = manager.getBundles();
        log.info("The number of installed bundles: {}", bundles.length);
        for (Bundle bundle : bundles) {
            log.info("Bundle Id : {}\tBundle Symbol : {}\tBundle location : {}",
                    bundle.getBundleId(), bundle.getSymbolicName(), bundle.getLocation());
        }
    }

//    @Test
//    public void coinContractInstallTest() throws IOException {
//        installByVersion(coinContract);
//    }

    @Test
    public void uninstallTest() {
        Bundle bundle = manager.getBundle(coinContract);
        if (bundle == null) {
            log.debug("Coin bundle does not exist");
        } else {
            manager.uninstall(coinContract);
        }
    }

//    private void installByVersion(ContractVersion contractVersion) throws IOException {
//        File coinFile = null;
//        if (manager.isContractFileExist(contractVersion)) {
//            coinFile = new File(config.getContractPath() + File.separator + contractVersion + ".jar");
//        } else {
//            coinFile = manager.downloader(contractVersion);
//        }
//
//        Assert.assertNotNull("Failed to download COIN-CONTRACT File on system", coinFile);
//
//        boolean verified = manager.verifyContractFile(coinFile, contractVersion);
//
//        Assert.assertTrue("Failed to verify contract file", verified);
//
//        try {
//            Bundle coinBundle = manager.install(contractVersion, true);
//            manager.start(coinBundle);
//            manager.registerServiceMap(contractVersion, coinBundle);
//            manager.inject(contractVersion);
//
//        } catch (IOException | BundleException | IllegalAccessException e) {
//            log.error(e.getMessage());
//        }
//
//        Bundle[] bundles = manager.getBundles();
//        Assert.assertTrue("Failed to install COIN-CONTRACT on osgi", bundles.length > 1);
//        printBundles();
//    }

    private ContractManager initContractManager() throws IOException, BundleException {
        config = new DefaultConfig();
        genesis = BlockChainTestUtils.getGenesis();
        systemProperties = BlockChainTestUtils.createDefaultSystemProperties();

        branchId = genesis.getBranchId();

        bcStore = BlockChainStoreBuilder.newBuilder(branchId)
                .withDataBasePath(config.getDatabasePath())
                .withProductionMode(config.isProductionMode())
                .setConsensusAlgorithm(null)
                .setBlockStoreFactory(PbftBlockStoreMock::new)
                .build();

        contractStore = bcStore.getContractStore();

        FrameworkConfig bootFrameworkConfig = new BootFrameworkConfig(config, branchId);
        FrameworkLauncher bootFrameworkLauncher = new BootFrameworkLauncher(bootFrameworkConfig);
        BundleService bundleService = new BundleServiceImpl(bootFrameworkLauncher.getBundleContext());

        ContractManager manager = ContractManagerBuilder.newInstance()
                .withGenesis(genesis)
                .withBundleManager(bundleService)
                .withDefaultConfig(config)
                .withContractStore(contractStore)
                .withLogStore(bcStore.getLogStore()) // is this logstore for what?
                .withSystemProperties(systemProperties)
                .build();

        Assert.assertNotNull("Manager is null", manager);
        return manager;
    }

}
