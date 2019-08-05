package io.yggdrash.core.blockchain.osgi;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.contract.BranchContract;
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


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

    private ContractExecutor executor;

    private static ContractVersion coinContract = ContractVersion.of("8c65bc05e107aab9ceaa872bbbb2d96d57811de4");

    @Before
    public void setUp() throws Exception {
        this.manager = initContractManager();
        this.executor = manager.getContractExecutor();
    }

    public void printBundles() {
        Bundle[] bundles = this.manager.getBundles(branchId);
        log.info("The number of installed bundles: {}", bundles.length);
        for (Bundle bundle : bundles) {
            log.info("Bundle Id : {}\tBundle Symbol : {}\tBundle location : {}",
                    bundle.getBundleId(), bundle.getSymbolicName(), bundle.getLocation());
        }
    }

    @Test
    public void coinContractInstallTest() {
        installByVersion(branchId, coinContract);
    }

    @Test
    public void multiBranchTest() throws IOException {
        generateNewCoinBranch();

        HashMap<BranchId, FrameworkLauncher> launcherMap = manager.getFrameworkHashMap();
        FrameworkLauncher coinFramework = launcherMap.get(coinBranchId);

        Assert.assertEquals("Invalid framework size", 2, launcherMap.size());
        Assert.assertNotNull("FrameworkLauncher is null", coinFramework);

        installByVersion(coinBranchId, coinContract);
    }

    @Test
    public void uninstallTest() {
        Bundle bundle = manager.getBundle(branchId, coinContract);
        if (bundle == null) {
            log.debug("Coin bundle does not exist");
        } else {
            manager.uninstall(branchId, coinContract);
        }
    }

    private void installByVersion(BranchId branchId, ContractVersion contractVersion) {
        File coinFile = null;
        if (manager.isContractFileExist(contractVersion)) {
            coinFile = new File(config.getContractPath() + File.separator + contractVersion + ".jar");
        } else {
            try {
                coinFile = manager.downloader(contractVersion);
            } catch (IOException e) {
                log.error(e.getMessage());
                manager.deleteContractFile(new File(config.getContractPath() + File.separator + contractVersion + ".jar"));
            }
        }

        Assert.assertNotNull("Failed to download COIN-CONTRACT File on system", coinFile);

        boolean verified = manager.verifyContractFile(coinFile, contractVersion);

        Assert.assertTrue("Failed to verify contract file", verified);

        try {
            Bundle coinBundle = manager.install(branchId, contractVersion, true);
            manager.start(coinBundle);
            manager.inject(branchId, contractVersion);
            manager.registerServiceMap(branchId, contractVersion, coinBundle);

        } catch (IOException | IllegalAccessException | BundleException e) {
            log.error(e.getMessage());
        }

        Bundle[] bundles = manager.getBundles(branchId);
        Map<String, Object> serviceMap = manager.getServiceMap();

        Assert.assertTrue("Failed to install COIN-CONTRACT on osgi", bundles.length > 1);
        Assert.assertNotNull("Failed to register COIN_CONTRACT on service map",
                serviceMap.get(contractVersion.toString()));

        printBundles();
    }

    private void generateNewCoinBranch() throws IOException {
        String filePath = Objects.requireNonNull(
                getClass().getClassLoader().getResource("branch-coin.json")).getFile();
        File coinBranchFile = new File(filePath);
        this.coinGenesis = GenesisBlock.of(new FileInputStream(coinBranchFile));
        this.coinBranchId = this.coinGenesis.getBranchId();

        FrameworkConfig bootFrameworkConfig = new BootFrameworkConfig(config, coinGenesis.getBranchId());
        FrameworkLauncher coinFramework = new BootFrameworkLauncher(bootFrameworkConfig);

        manager.addFramework(coinFramework);
    }

    private ContractManager initContractManager() throws IOException, BundleException {
        this.config = new DefaultConfig();
        this.genesis = BlockChainTestUtils.getGenesis();
        this.systemProperties = BlockChainTestUtils.createDefaultSystemProperties();

        this.branchId = genesis.getBranchId();


        this.bcStore = BlockChainStoreBuilder.newBuilder(branchId)
                .withDataBasePath(config.getDatabasePath())
                .withProductionMode(config.isProductionMode())
                .setConsensusAlgorithm(null)
                .setBlockStoreFactory(PbftBlockStoreMock::new)
                .build();

        this.contractStore = bcStore.getContractStore();

        FrameworkConfig bootFrameworkConfig = new BootFrameworkConfig(config, branchId);
        FrameworkLauncher bootFrameworkLauncher = new BootFrameworkLauncher(bootFrameworkConfig);
        BundleService bundleService = new BundleServiceImpl();

        List<BranchContract> genesisContractList = genesis.getBranch().getBranchContracts();

        assert genesisContractList.size() > 0;

        ContractManager manager = ContractManagerBuilder.newInstance()
                .withGenesis(genesis)
                .withBootFramework(bootFrameworkLauncher)
                .withBundleManager(bundleService)
                .withDefaultConfig(config)
                .withContractStore(contractStore)
                .withLogStore(bcStore.getLogStore()) // is this logstore for what?
                .withSystemProperties(systemProperties)
                .build();

        assert manager != null;
        assert manager.getContractExecutor() != null;

        return manager;

    }

}
