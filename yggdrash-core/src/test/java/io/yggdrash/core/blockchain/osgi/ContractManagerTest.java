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

    private static ContractVersion coinContract = ContractVersion.of("a88ae404e837cd1d6e8b9a5a91f188da835ccb56");
    ContractVersion notExistedVersion = ContractVersion.of(Hex.encodeHexString("Wrong ContractVersion".getBytes()));

    @Before
    public void setUp() throws Exception {
        manager = initContractManager();
    }

    @Test
    public void loadBundleTest() throws BundleException {
        int numOfBundles = manager.getBundles().length;
        manager.loadBundle(coinContract);
        Assert.assertNotNull(manager.getBundle(coinContract));
        Assert.assertEquals(numOfBundles + 1, manager.getBundles().length);
        manager.uninstall(coinContract);
    }

    @Test
    public void loadBundleDownloadFailTest() {
        // not found in s3 repository.
        int numOfBundles = manager.getBundles().length;
        manager.loadBundle(notExistedVersion);
        Assert.assertEquals(numOfBundles, manager.getBundles().length);
    }

    private ContractManager initContractManager() {
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

        new Downloader(config);

        return ContractManagerBuilder.newInstance()
                .withGenesis(genesis)
                .withBundleManager(bundleService)
                .withDefaultConfig(config)
                .withContractStore(contractStore)
                .withLogStore(bcStore.getLogStore()) // is this logstore for what?
                .withSystemProperties(systemProperties)
                .build();
    }

    private void printBundles() {
        Bundle[] bundles = manager.getBundles();
        log.info("The number of installed bundles: {}", bundles.length);
        for (Bundle bundle : bundles) {
            log.info("Bundle Id : {}\tBundle Symbol : {}\tBundle location : {}",
                    bundle.getBundleId(), bundle.getSymbolicName(), bundle.getLocation());
        }
    }

    @Test
    public void isExistContractTest() {
        boolean notExist = Downloader.verifyUrl(ContractVersion.of("wrongVersion".getBytes()));
        boolean exist = Downloader.verifyUrl(coinContract);

        Assert.assertFalse(notExist);
        Assert.assertTrue(exist);
    }

}
