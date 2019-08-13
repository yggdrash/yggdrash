package io.yggdrash.core.blockchain.osgi;

import com.google.gson.JsonObject;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.contract.BranchContract;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.Log;
import io.yggdrash.core.blockchain.SystemProperties;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import io.yggdrash.core.blockchain.osgi.framework.BundleService;
import io.yggdrash.core.blockchain.osgi.framework.FrameworkLauncher;
import io.yggdrash.core.blockchain.osgi.service.VersioningContract;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.runtime.result.BlockRuntimeResult;
import io.yggdrash.core.runtime.result.TransactionRuntimeResult;
import io.yggdrash.core.store.ContractStore;
import io.yggdrash.core.store.LogStore;
import org.apache.commons.io.IOUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.framework.launch.Framework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class ContractManager {
    private static final Logger log = LoggerFactory.getLogger(ContractManager.class);

    private final BranchId bootBranchId;
    private final ContractStore contractStore;
    private final LogStore logStore;

    private final String contractRepositoryUrl;
    private final String contractPath;
    private final SystemProperties systemProperties;
    private final ContractExecutor contractExecutor;

    private final BundleService bundleService;
    private final Framework framework;

    private final DefaultConfig defaultConfig;
    private final GenesisBlock genesis;


    private Map<String, Object> serviceMap;

    ContractManager(GenesisBlock genesis, FrameworkLauncher frameworkLauncher, BundleService bundleService, DefaultConfig defaultConfig,
                    ContractStore contractStore, LogStore logStore, SystemProperties systemProperties) {

        this.bootBranchId = genesis.getBranchId();
        this.contractStore = contractStore;
        this.logStore = logStore;

        this.contractPath = defaultConfig.getContractPath();
        this.contractRepositoryUrl = defaultConfig.getContractRepositoryUrl();
        this.systemProperties = systemProperties;

        this.contractExecutor = new ContractExecutor(contractStore, logStore);

        this.framework = frameworkLauncher.getFramework();

        this.bundleService = bundleService;
        this.defaultConfig = defaultConfig;
        this.genesis = genesis;

        this.serviceMap = new HashMap<>();

        initBootBundles();

        VersioningContract.VersioningContractService service = new VersioningContract.VersioningContractService();
        serviceMap.put(ContractConstants.VERSIONING_TRANSACTION, service);

        try {
            contractExecutor.injectField(new ArrayList<>(serviceMap.values()));
        } catch (IllegalAccessException e) {
            log.error(e.getMessage());
        } catch (ExecutorException e) {
            e.printStackTrace();
        }
    }

    private void initBootBundles() {

        List<BranchContract> branchContractList = this.getContractList();

        if (branchContractList.isEmpty()) {
            log.warn("This branch {} has no any contract.", bootBranchId);
            return;
        }

        for (BranchContract branchContract : branchContractList) {
            ContractVersion contractVersion = branchContract.getContractVersion();

            File contractFile = null;
            if (isContractFileExist(contractVersion)) {
                contractFile = new File(contractFilePath(contractVersion));
            } else {
                try {
                    contractFile = downloader(contractVersion);
                } catch (IOException e) {
                    log.error("Failed to download Contract File {}.jar on system with {}", contractVersion, e.getMessage());
                    deleteContractFile(new File(contractFilePath(contractVersion)));
                }
            }

            verifyContractFile(contractFile, contractVersion);

            Bundle bundle = getBundle(contractVersion);

            if (bundle == null) {
                try {
                    bundle = install(bootBranchId, contractVersion, true);
                } catch (IOException e) {
                    log.error("ContractFile has an Error with {}", e.getMessage());
                    continue;
                } catch (BundleException e) {
                    log.error("ContractFile {} failed to install with {}", contractVersion, e.getMessage());
                    continue;
                }
            }

            try {
                start(bundle);
            } catch (BundleException e) {
                log.error("Bundle {} failed to start with {}", bundle.getSymbolicName(), e.getMessage());
                continue;
            }

            registerServiceMap(contractVersion, bundle);
        }
    }

    /**
     * get contract list from branchStore or genesis block.
     * @return contractList
     */
    private List<BranchContract> getContractList() {
        if (contractStore.getBranchStore().getBranchContacts().isEmpty()) {
            return genesis.getBranch().getBranchContracts();
        }
        return contractStore.getBranchStore().getBranchContacts();
    }

    public void inject(ContractVersion contractVersion) {
        Object service = serviceMap.get(contractVersion.toString());
        try {
            contractExecutor.inject(service);
        } catch (ExecutorException e) {
            log.error("This service that contract version {} is not registered", contractVersion);
        } catch (IllegalAccessException e) {
            log.error(e.getMessage());
        }
    }

    public Bundle getBundle(ContractVersion contractVersion) {
        return bundleService.getBundle(framework.getBundleContext(), contractVersion);
    }

    public Bundle[] getBundles() {
        return bundleService.getBundles(framework.getBundleContext());
    }

    public Bundle install(BranchId branchId, ContractVersion contractVersion, File contractFile, boolean isSystem) throws IOException, BundleException {
        Bundle bundle = bundleService.getBundle(framework.getBundleContext(), contractVersion);

        try (JarFile jarFile = new JarFile(contractFile)) {
            if (bundle != null && isInstalledContract(jarFile, bundle)) {
                log.debug("Already installed bundle {}", contractVersion);
                return bundle;
            }

            if (verifyManifest(jarFile.getManifest())) {
                log.debug("Installing  bundle {}", contractVersion);
                return bundleService.install(framework.getBundleContext(), contractVersion, contractFile, isSystem);
            }
        }
        return null;
    }

    public Bundle install(BranchId branchId, ContractVersion contractVersion, boolean isSystem) throws IOException, BundleException {
        File contractFile = new File(defaultConfig.getContractPath() + File.separator + contractVersion + ".jar");
        return install(branchId, contractVersion, contractFile, isSystem);
    }

    private boolean isInstalledContract(JarFile jarFile, Bundle bundle) throws IOException {
        List<String> bundleKeys = Collections.list(bundle.getHeaders().keys());
        Manifest m = jarFile.getManifest();

        for (String key : bundleKeys) {
            if (!m.getMainAttributes().getValue(key).equals(bundle.getHeaders().get(key))) {
                return false;
            }
        }
        return true;
    }

    public void registerServiceMap(ContractVersion contractVersion, Bundle bundle) {
        BundleContext context = framework.getBundleContext();
        Object service = context.getService(bundle.getRegisteredServices()[0]);
        this.serviceMap.put(contractVersion.toString(), service);

    }

    public void uninstall(BranchId branchId, ContractVersion contractVersion) {
        try {
            bundleService.uninstall(framework.getBundleContext(), contractVersion);
        } catch (BundleException e) {
            log.error(e.getMessage());
        }
    }

    public void start(BranchId branchId, ContractVersion contractVersion) throws BundleException {
        Bundle bundle = bundleService.getBundle(framework.getBundleContext(), contractVersion);
        start(bundle);
    }

    public void start(Bundle bundle) throws BundleException {
        bundleService.start(bundle);
    }

    public Long getStateSize() { // TODO for BranchController -> remove this
        return contractStore.getStateStore().getStateSize();
    }

    ContractExecutor getContractExecutor() {
        return contractExecutor;
    }

    public String getContractPath() {
        return contractPath;
    }

    public Log getLog(long index) {
        return contractExecutor.getLog(index);
    }

    public List<Log> getLogs(long start, long offset) {
        return contractExecutor.getLogs(start, offset);
    }

    public long getCurLogIndex() {
        return contractExecutor.getCurLogIndex();
    }

    private boolean verifyManifest(Manifest manifest) {
        String manifestVersion = manifest.getMainAttributes().getValue("Bundle-ManifestVersion");
        String bundleSymbolicName = manifest.getMainAttributes().getValue("Bundle-SymbolicName");
        String bundleVersion = manifest.getMainAttributes().getValue("Bundle-Version");
        return verifyManifest(manifestVersion, bundleSymbolicName, bundleVersion);
    }

    private boolean verifyManifest(String manifestVersion, String bundleSymbolicName, String bundleVersion) {
        if (!"2".equals(manifestVersion)) {
            log.error("Must set Bundle-ManifestVersion to 2");
            return false;
        }
        if (bundleSymbolicName == null || "".equals(bundleSymbolicName)) {
            log.error("Must set Bundle-SymbolicName");
            return false;
        }

        if (bundleVersion == null || "".equals(bundleVersion)) {
            log.error("Must set Bundle-Version");
            return false;
        }

        return true;
    }

    public List<ContractStatus> searchContracts(BranchId branchId) {
        List<ContractStatus> result = new ArrayList<>();
        Bundle[] bundleList = bundleService.getBundles(framework.getBundleContext());
        for (Bundle bundle : bundleList) {
            result.add(getContractStatus(bundle));
        }
        return result;
    }

    private ContractStatus getContractStatus(Bundle bundle) {
        Dictionary<String, String> header = bundle.getHeaders();
        int serviceCnt = bundle.getRegisteredServices() == null ? 0 : bundle.getRegisteredServices().length;

        Version v = bundle.getVersion();
        return new ContractStatus(
                bundle.getSymbolicName(),
                String.format("%s.%s.%s", v.getMajor(), v.getMinor(), v.getMicro()),
                header.get("Bundle-Vendor"),
                header.get("Bundle-Description"),
                bundle.getBundleId(),
                bundle.getLocation(),
                bundle.getState(),
                serviceCnt
        );
    }

    public Object query(BranchId branchId, String contractVersion, String methodName, JsonObject params) {
        return contractExecutor.query(serviceMap, contractVersion, methodName, params);
    }

    public BlockRuntimeResult executeTxs(ConsensusBlock nextBlock) {
        return contractExecutor.executeTxs(serviceMap, nextBlock);
    }

    public TransactionRuntimeResult executeTx(Transaction tx) {
        return contractExecutor.executeTx(serviceMap, tx);

    }

    public void commitBlockResult(BlockRuntimeResult result) {
        contractExecutor.commitBlockResult(result);
    }

    public void close() {
        contractStore.close();
        logStore.close();
    }

    public boolean isContractFileExist(ContractVersion version) {

        File contractDir = new File(this.getContractPath());
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

    public File downloader(ContractVersion version) throws IOException {

        int bufferSize = 1024;

        try (OutputStream outputStream = new BufferedOutputStream(
                new FileOutputStream(contractFilePath(version)))) {
            log.info("-------Download Start------");
            URL url = new URL(this.contractRepositoryUrl + version + ".jar");
            byte[] buf = new byte[bufferSize];
            int byteWritten = 0;

            URLConnection connection = url.openConnection();
            InputStream inputStream = connection.getInputStream();

            int byteRead;
            while ((byteRead = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, byteRead);
                byteWritten += byteRead;
            }

            log.info("Download Successfully.");
            log.info("Contract Version : {}\t of bytes : {}", version, byteWritten);
            log.info("-------Download End--------");
        }
        return new File(contractFilePath(version));

    }

    public boolean verifyContractFile(File contractFile, ContractVersion contractVersion) {
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

    public boolean deleteContractFile(File contractFile) {
        return contractFile.delete();
    }

    private String contractFilePath(ContractVersion contractVersion) {
        return this.contractPath + File.separator + contractVersion + ".jar";
    }

    public Map<String, Object> getServiceMap() {
        return serviceMap;
    }
}
