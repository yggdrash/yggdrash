package io.yggdrash.core.blockchain.osgi;

import com.google.gson.JsonObject;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.ContractTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.contract.core.ContractEvent;
import io.yggdrash.contract.core.ExecuteStatus;
import io.yggdrash.contract.core.channel.ContractEventType;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.SystemProperties;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionBuilder;
import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import io.yggdrash.core.blockchain.osgi.framework.BootFrameworkConfig;
import io.yggdrash.core.blockchain.osgi.framework.BootFrameworkLauncher;
import io.yggdrash.core.blockchain.osgi.framework.BundleService;
import io.yggdrash.core.blockchain.osgi.framework.BundleServiceImpl;
import io.yggdrash.core.blockchain.osgi.framework.FrameworkConfig;
import io.yggdrash.core.blockchain.osgi.framework.FrameworkLauncher;
import io.yggdrash.core.blockchain.osgi.service.ContractProposal;
import io.yggdrash.core.net.NodeStatus;
import io.yggdrash.core.net.NodeStatusMock;
import io.yggdrash.core.runtime.result.TransactionRuntimeResult;
import io.yggdrash.core.store.BlockChainStore;
import io.yggdrash.core.store.BlockChainStoreBuilder;
import io.yggdrash.core.store.ContractStore;
import io.yggdrash.core.store.PbftBlockStoreMock;
import io.yggdrash.core.wallet.Wallet;
import org.apache.commons.codec.binary.Hex;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;


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

    private NodeStatus nodeStatus;

    @Before
    public void setUp() throws Exception {
        nodeStatus = NodeStatusMock.mock;
        manager = initContractManager();
        manager.setNodeStatus(nodeStatus);
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


    @Test
    public void versioningEventTest() throws Exception {
        // best case test.
        ContractEvent activateAgreeEvent =
                createEvent(ContractEventType.AGREE, coinContract.toString(), "ACTIVATE");
        ContractEvent activateApplyEvent =
                createEvent(ContractEventType.APPLY, coinContract.toString(), "ACTIVATE");

        int originNumberOfBundles = getNumberOfBundles();
        int originNumberOfBranchContracts = getNumberOfBranchContracts();

        // download file to temp folder: contractPath/tmp/contractVersion.jar
        manager.endBlock(activateAgreeEvent);
        Assert.assertTrue(isFileDownloaded(coinContract.toString()));

        // load new contract.
        manager.endBlock(activateApplyEvent);
        Assert.assertEquals(originNumberOfBundles + 1, getNumberOfBundles());
        Assert.assertEquals(originNumberOfBranchContracts + 1, getNumberOfBranchContracts());


        Transaction tx = generateTx(BigInteger.valueOf(10), coinContract);
        TransactionRuntimeResult res = manager.executeTx(tx);

        Assert.assertEquals(ExecuteStatus.ERROR, res.getReceipt().getStatus());
        Assert.assertTrue(res.getReceipt().getLog().contains("Insufficient funds"));

        ContractEvent deactivateAgreeEvent =
                createEvent(ContractEventType.AGREE, coinContract.toString(), "DEACTIVATE");
        ContractEvent deactivateApplyEvent =
                createEvent(ContractEventType.APPLY, coinContract.toString(), "DEACTIVATE");

        // do nothing deactivate agree event.
        manager.endBlock(deactivateAgreeEvent);

        // unload contract
        manager.endBlock(deactivateApplyEvent);
        Assert.assertEquals(originNumberOfBundles, getNumberOfBundles());
        Assert.assertEquals(originNumberOfBranchContracts, getNumberOfBranchContracts());

        // transaction test : not allowed.
        TransactionRuntimeResult res2 = manager.executeTx(tx);
        Assert.assertEquals(ExecuteStatus.ERROR, res2.getReceipt().getStatus());
        Assert.assertTrue(res2.getReceipt().getLog().contains("ContractVersion doesn't exist"));

    }

    private ContractEvent createEvent(ContractEventType eventType, String proposalVersion, String proposalType) {
        ContractProposal proposal = createContractProposal(proposalVersion, proposalType);
        return new ContractEvent(eventType, proposal, ContractConstants.VERSIONING_CONTRACT.toString());

    }

    private ContractProposal createContractProposal(String proposalVersion, String proposalType) {
        String txId = "aeb57125e362e49eec4a737c18348f6e9bd4ea68962dfd7671bc8552eaa0c95f";
        String proposer = "77283a04b3410fe21ba5ed04c7bd3ba89e70b78c";
        String sourceUrl = "http://github.com/yggdrash";
        String buildVersion = "1.0.0";
        long votePeriod = 10L;
        long applyPeriod = 10L;

        Set<String> validatorSet = new HashSet<>();
        long blockHeight = 10L;

        return new ContractProposal(
                txId, proposer, proposalVersion, sourceUrl, buildVersion, blockHeight,
                votePeriod, applyPeriod, validatorSet, proposalType);
    }

    private boolean isFileDownloaded(String version) {
        String tmpFile = config.getContractPath() + File.separator + "tmp" + File.separator + version + ".jar";
        File file = new File(tmpFile);
        return file.isFile();
    }

    private int getNumberOfBundles() {
        return manager.getBundles().length;
    }

    private int getNumberOfBranchContracts() {
        return contractStore.getBranchStore().getBranchContacts().size();
    }

    private Transaction generateTx(BigInteger amount, ContractVersion contractVersion) throws Exception {
        Wallet wallet = null;
        wallet = ContractTestUtils.createTestWallet("dea328146c7248231a5bcafdeea12019a2f5dc58.json");
        JsonObject txBody =
                ContractTestUtils.transferTxBodyJson(TestConstants.TRANSFER_TO, amount, contractVersion);
        TransactionBuilder builder = new TransactionBuilder();
        return builder.setTxBody(txBody).setWallet(wallet).setBranchId(branchId).build();
    }

}
