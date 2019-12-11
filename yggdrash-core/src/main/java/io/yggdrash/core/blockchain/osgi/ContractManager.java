package io.yggdrash.core.blockchain.osgi;

import com.google.gson.JsonObject;
import io.yggdrash.common.Sha3Hash;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.contract.BranchContract;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.contract.core.ContractEvent;
import io.yggdrash.contract.core.Receipt;
import io.yggdrash.contract.core.ReceiptImpl;
import io.yggdrash.contract.core.channel.ContractEventType;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Log;
import io.yggdrash.core.blockchain.LogIndexer;
import io.yggdrash.core.blockchain.SystemProperties;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import io.yggdrash.core.blockchain.osgi.framework.BundleService;
import io.yggdrash.core.blockchain.osgi.service.ContractProposal;
import io.yggdrash.core.blockchain.osgi.service.ProposalType;
import io.yggdrash.core.blockchain.osgi.service.VersioningContract;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.net.NodeStatus;
import io.yggdrash.core.runtime.result.BlockRuntimeResult;
import io.yggdrash.core.runtime.result.TransactionRuntimeResult;
import io.yggdrash.core.store.ContractStore;
import io.yggdrash.core.store.LogStore;
import org.apache.commons.io.IOUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

public class ContractManager implements ContractEventListener {
    private static final Logger log = LoggerFactory.getLogger(ContractManager.class);

    private final BranchId bootBranchId;
    private final ContractStore contractStore;
    private final LogStore logStore;

    private final String contractPath;
    private final SystemProperties systemProperties;
    private final ContractExecutor contractExecutor;

    private final BundleService bundleService;
    private final LogIndexer logIndexer;

    private final DefaultConfig defaultConfig;
    private final GenesisBlock genesis;

    private NodeStatus nodeStatus;

    private Map<String, Object> serviceMap;

    ContractManager(GenesisBlock genesis, BundleService bundleService, DefaultConfig defaultConfig,
                    ContractStore contractStore, LogStore logStore, SystemProperties systemProperties) {

        this.bootBranchId = genesis.getBranchId();
        this.contractStore = contractStore;
        this.logStore = logStore;

        this.contractPath = defaultConfig.getContractPath();

        this.systemProperties = systemProperties;

        this.logIndexer = new LogIndexer(logStore, contractStore.getReceiptStore());
        this.contractExecutor = new ContractExecutor(contractStore, logIndexer);

        this.bundleService = bundleService;
        this.defaultConfig = defaultConfig;
        this.genesis = genesis;

        this.serviceMap = new HashMap<>();

        initBootBundles();
        initNodeContract();

    }

    public void setNodeStatus(NodeStatus nodeStatus) {
        this.nodeStatus = nodeStatus;
    }

    public void commitBlockResult(BlockRuntimeResult result) {
        contractExecutor.commitBlockResult(result);
    }

    @Override
    public void endBlock(ContractEvent event) {
        versioningContractEventHandler(event);
    }

    @Override
    public void endBlock(BlockRuntimeResult result, ContractEvent event) {
        // todo : remove this if not needed.
    }

    private void versioningContractEventHandler(ContractEvent event) {
        ContractEventType eventType = event.getType();
        ContractProposal proposal = (ContractProposal) event.getItem();
        ContractVersion proposalVersion = ContractVersion.of(proposal.getProposalVersion());

        ProposalType proposalType = proposal.getProposalType();
        log.debug("VersioningContract EventHandler : ContractEventType={}, ProposalVersion={}, ProposalType={}",
                eventType, proposalVersion, proposalType);

        if (nodeStatus.isSyncStatus()) {
            log.debug("nodeStatus isSyncStatus -> " + nodeStatus.isSyncStatus());
            nodeStatus.update();
        }
        if (proposalType.equals(ProposalType.ACTIVATE)) {
            proposalActivateHandler(eventType, proposalVersion);
        } else if (proposalType.equals(ProposalType.DEACTIVATE)) {
            proposalDeactivateHandler(eventType, proposalVersion);
        }

        if (!nodeStatus.isUpStatus()) {
            log.debug("nodeStatus isUpStatus -> " + nodeStatus.isUpStatus());
            nodeStatus.up();
        }
    }

    private void proposalActivateHandler(ContractEventType eventType, ContractVersion proposalVersion) {
        try {
            switch (eventType) {
                case AGREE:
                    // download contract file to contract tmp folder
                    if (Downloader.verifyUrl(proposalVersion)) {
                        Downloader.downloadContract(String.format("%s/%s", contractPath, "tmp"), proposalVersion);
                    }
                    break;
                case APPLY:
                    // pkg version check
                    if (isPackageAvailable(proposalVersion)) {
                        // copy file
                        copyContractFile(proposalVersion);
                        // load bundle service
                        loadBundle(proposalVersion);
                        // save into branchStore.
                        addNewBranchContract(bundleService.getBundle(proposalVersion), proposalVersion);
                    } else {
                        log.warn("proposal contract {} dose not installed. Already exist package version",
                                proposalVersion);
                    }
                    break;
                default:
                    log.info("Not defined event type in version contract");
                    break;
            }
        } catch (Exception e) {
            log.error("VersioningContract event failed. {} ", e.getMessage());
        }
    }

    private void proposalDeactivateHandler(ContractEventType eventType, ContractVersion proposalVersion) {
        try {
            switch (eventType) {
                case AGREE:
                    log.info("[DeactivateHandler]\teventType: {}, proposalVersion: {}", eventType, proposalVersion);
                    break;
                case APPLY:
                    // unload process.
                    unloadBundle(proposalVersion);
                    deleteBranchContract(proposalVersion.toString());
                    log.info("[DeactivateHandler]\teventType: {}, proposalVersion: {}", eventType, proposalVersion);
                    break;
                default:
                    log.info("Not defined event type in version contract");
                    break;
            }
        } catch (Exception e) {
            log.error("VersioningContract event failed. {} ", e.getMessage());
        }
    }

    private void addNewBranchContract(Bundle newBundle, ContractVersion proposalVersion) {

        Dictionary<String, String> bundleMeta = newBundle.getHeaders();
        List<BranchContract> branchContracts = getBranchContractListByName(bundleMeta.get("Bundle-Name"));

        JsonObject branchContractJson = null;
        if (branchContracts.isEmpty()) {
            // create new branch contract from bundle metadata
            branchContractJson = new JsonObject();
            branchContractJson.add("init", new JsonObject());
            branchContractJson.addProperty("name", bundleMeta.get("Bundle-Name"));
            branchContractJson.addProperty("description", bundleMeta.get("Bundle-Description"));
            branchContractJson.addProperty("property", "");
            branchContractJson.addProperty("isSystem", false);
            branchContractJson.addProperty("contractVersion", proposalVersion.toString());
        } else {
            // create new branch contract from already exist contract data.
            branchContractJson = branchContracts.get(0).getJson().deepCopy();
            branchContractJson.addProperty("contractVersion", proposalVersion.toString());
        }
        // save new branch contract into branch store
        BranchContract newBranchContract = BranchContract.of(branchContractJson);
        contractStore.getBranchStore().addBranchContract(newBranchContract);
    }

    private void deleteBranchContract(String contractVersion) {
        contractStore.getBranchStore().removeBranchContract(contractVersion);
    }

    private void initBootBundles() {

        List<BranchContract> branchContractList = this.getBranchContractList();

        if (branchContractList.isEmpty()) {
            log.warn("This branch {} has no any contract.", bootBranchId);
            return;
        }

        for (BranchContract branchContract : branchContractList) {
            ContractVersion contractVersion = branchContract.getContractVersion();
            loadBundle(contractVersion);
        }
    }

    private void initNodeContract() {
        VersioningContract service = new VersioningContract();
        serviceMap.put(ContractConstants.VERSIONING_CONTRACT.toString(), service);
        contractExecutor.injectNodeContract(service);

    }

    public void loadBundle(ContractVersion contractVersion) {
        log.debug("LoadBundle : contractVersion={}", contractVersion);
        // step 1. exist file and download file.
        File contractFile = null;
        if (isContractFileExist(contractVersion)) {
            contractFile = new File(contractFilePath(contractVersion));
        } else {
            contractFile = Downloader.downloadContract(contractPath, contractVersion);
        }

        if (!verifyContractFile(contractFile, contractVersion)) {
            return;
        }

        // step 2. install and start
        Bundle bundle = getBundle(contractVersion);

        if (bundle == null) {
            try {
                bundle = bundleService.install(contractVersion, contractFile);
            } catch (IOException | BundleException e) {
                // Mark : throw runtime error?
                log.error("ContractFile {} failed to install with {}", contractVersion, e.getMessage());
                return;
            }
        }

        try {
            bundleService.start(contractVersion);
        } catch (BundleException e) {
            log.error("Bundle {} failed to start with {}", bundle.getSymbolicName(), e.getMessage());
            return;
        }

        // step 3. register service and injectBundle
        registerServiceMap(contractVersion, bundle);
        injectBundle(bundle);
    }

    private void unloadBundle(ContractVersion contractVersion) throws Exception {
        Bundle bundle = bundleService.getBundle(contractVersion);
        if (bundle == null) {
            throw new Exception(String.format("contract %s does not exist", contractVersion));
        } else {
            bundleService.stop(contractVersion);
            bundleService.uninstall(contractVersion);
            serviceMap.remove(contractVersion.toString());
            contractExecutor.flush(contractVersion.toString());
        }
    }

    /**
     * get contract list from branchStore or genesis block.
     * @return contractList
     */
    private List<BranchContract> getBranchContractList() {
        if (contractStore.getBranchStore().getBranchContacts().isEmpty()) {
            return genesis.getBranch().getBranchContracts();
        }
        return contractStore.getBranchStore().getBranchContacts();
    }

    private List<BranchContract> getBranchContractListByName(String contractName) {
        return contractStore.getBranchStore().getBranchContractsByName(contractName);
    }

    private void injectBundle(Bundle bundle) {
        Object service = bundleService.getBundleService(bundle);
        if (service == null) {
            log.error("No available service in bundle {}", bundle.getSymbolicName());
            return;
        }
        contractExecutor.injectBundleContract(bundle, service);
    }

    private void registerServiceMap(ContractVersion contractVersion, Bundle bundle) {
        Object service = bundleService.getBundleService(bundle);
        this.serviceMap.put(contractVersion.toString(), service);
    }

    // bundle service actions.
    public Bundle getBundle(ContractVersion contractVersion) {
        return bundleService.getBundle(contractVersion);
    }

    public Bundle[] getBundles() {
        return bundleService.getBundles();
    }

    public List<Bundle> getBundlesByName(String contractName) {
        return bundleService.getBundlesByName(contractName);
    }

    private void install(ContractVersion contractVersion) throws IOException, BundleException {
        File contractFile = new File(contractFilePath(contractVersion));
        bundleService.install(contractVersion, contractFile);
    }

    public void uninstall(ContractVersion contractVersion) {
        try {
            bundleService.uninstall(contractVersion);
        } catch (BundleException e) {
            log.error(e.getMessage());
        }
    }

    public void stop(ContractVersion contractVersion) throws BundleException {
        bundleService.stop(contractVersion);
    }

    public void start(ContractVersion contractVersion) throws BundleException {
        bundleService.start(contractVersion);
    }

    public List<ContractStatus> searchContracts() {
        // 시스템 번들을 출력할 필요가 있는지?
        return bundleService.getContractList();
    }

    // Log Indexer Services
    public Log getLog(long index) {
        return logIndexer.getLog(index);
    }

    public List<Log> getLogs(long start, long offset) {
        return logIndexer.getLogs(start, offset);
    }

    public long getCurLogIndex() {
        return logIndexer.curIndex();
    }

    // Executor Services
    public Object query(String contractVersion, String methodName, JsonObject params) throws Exception {
        return contractExecutor.query(serviceMap, contractVersion, methodName, params);
    }

    public Sha3Hash getOriginStateRootHash() {
        return contractStore.getStateStore().contains("stateRoot")
                ? new Sha3Hash(contractStore.getStateStore().get("stateRoot").get("stateHash").getAsString())
                : new Sha3Hash(Constants.EMPTY_HASH);
    }

    public JsonObject getOriginStateRoot() {
        return contractStore.getStateStore().contains("stateRoot")
                ? contractStore.getStateStore().get("stateRoot") : new JsonObject();
    }

    public BlockRuntimeResult executeTxs(ConsensusBlock nextBlock) {
        return contractExecutor.executeTxs(serviceMap, nextBlock);
    }

    public BlockRuntimeResult executeTxs(List<Transaction> txs) {
        return contractExecutor.executeTxs(serviceMap, txs);
    }

    //TODO executeTx should executed only by checkTx method
    public TransactionRuntimeResult executeTx(Transaction tx) {
        return contractExecutor.executeTx(serviceMap, tx);
    }

    public Receipt checkTx(Transaction tx) {
        if (defaultConfig.isCheckTxMode()) {
            TransactionRuntimeResult txResult = executeTx(tx);
            Receipt receipt = txResult.getReceipt();
            log.trace("[CheckTx] txHash={}, status={}, log={}",
                    tx.getHash().toString(), receipt.getStatus(), receipt.getLog());
            return receipt;
        }
        return new ReceiptImpl();
    }

    // file actions.
    public boolean isContractFileExist(ContractVersion version) {
        File contractDir = new File(contractPath);
        if (!contractDir.exists()) {
            contractDir.mkdirs();
            return false;
        }

        File contractFile = new File(contractFilePath(version));
        if (!contractFile.canRead()) {
            contractFile.setReadable(true, false);
        }

        return contractFile.isFile();
    }

    private boolean verifyContractFile(File contractFile, ContractVersion contractVersion) {
        // Contract Path + contract Version + .jar
        // check contractVersion Hex
        try (InputStream is = new FileInputStream(contractFile)) {
            byte[] contractBinary = IOUtils.toByteArray(is);
            ContractVersion checkVersion = ContractVersion.of(contractBinary);
            return contractVersion.toString().equals(checkVersion.toString());
        } catch (IOException e) {
            log.error(e.getMessage());
            return false;
        }
    }

    private String contractFilePath(ContractVersion contractVersion) {
        return this.contractPath + File.separator + contractVersion + ".jar";
    }

    private String contractTempFilePath(ContractVersion contractVersion) {
        return this.contractPath + File.separator + "tmp" + File.separator + contractVersion + ".jar";
    }

    private void copyContractFile(ContractVersion proposalVersion) throws IOException {
        Path tmp = Paths.get(String.format("%s/%s/%s", contractPath, "tmp", proposalVersion + ".jar"));
        Path origin = Paths.get(contractPath);
        Files.copy(tmp, origin.resolve(tmp.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    }

    private boolean isPackageAvailable(ContractVersion proposalVersion) throws IOException {
        File file = new File(contractTempFilePath(proposalVersion));

        if (verifyContractFile(file, proposalVersion)) {
            try (JarFile jarFile = new JarFile(file)) {
                String contractName = jarFile.getManifest().getMainAttributes().getValue("Bundle-Name");
                String pacakgeVersion = jarFile.getManifest().getMainAttributes().getValue("Bundle-Version");
                List<Bundle> bundles = getBundlesByName(contractName);
                return !bundles.stream()
                        .anyMatch(bundle -> bundle.getHeaders().get("Bundle-Version").equals(pacakgeVersion));
            }
        }

        return false;
    }

    public void close() {
        contractStore.close();
        logStore.close();
    }

}
