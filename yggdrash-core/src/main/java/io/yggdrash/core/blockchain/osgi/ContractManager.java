package io.yggdrash.core.blockchain.osgi;

import com.google.gson.JsonObject;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.contract.BranchContract;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.core.blockchain.SystemProperties;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.genesis.GenesisBlock;
import io.yggdrash.core.blockchain.osgi.framework.BundleService;
import io.yggdrash.core.blockchain.osgi.framework.FrameworkLauncher;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.runtime.result.BlockRuntimeResult;
import io.yggdrash.core.runtime.result.TransactionRuntimeResult;
import io.yggdrash.core.store.ContractStore;
import io.yggdrash.core.store.LogStore;
import org.apache.commons.io.IOUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundlePermission;
import org.osgi.framework.CapabilityPermission;
import org.osgi.framework.PackagePermission;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.condpermadmin.BundleLocationCondition;
import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;
import org.osgi.service.permissionadmin.PermissionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.ReflectPermission;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PropertyPermission;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class ContractManager {
    private static final Logger log = LoggerFactory.getLogger(ContractManager.class);

    private static final String SUFFIX_SYSTEM_CONTRACT = "contract/system";
    private static final String SUFFIX_USER_CONTRACT = "contract/user";

    private final String bootBranchId;              // bootBranch. will remove soon.
    private final ContractStore contractStore;
    private final LogStore logStore;

    private final String contractRepositoryUrl;
    private final String databasePath;
    private final String contractPath;
    private final SystemProperties systemProperties;
    private final Map<String, Object> serviceMap; // contractVersion, service of bundle

    private ContractExecutor contractExecutor;

    private final BundleService bundleService;
    private final HashMap<String, FrameworkLauncher> frameworkHashMap = new HashMap<>();
    private final DefaultConfig defaultConfig;
    private final GenesisBlock genesis;

    ContractManager(GenesisBlock genesis, FrameworkLauncher frameworkLauncher, BundleService bundleService, DefaultConfig defaultConfig,
                    ContractStore contractStore, LogStore logStore, SystemProperties systemProperties) {

        this.bootBranchId = genesis.getBranchId().toString();
        this.contractStore = contractStore;
        this.logStore = logStore;

        this.databasePath = defaultConfig.getDatabasePath();
        this.contractPath = defaultConfig.getContractPath();
        this.contractRepositoryUrl = defaultConfig.getContractRepositoryUrl();

        this.systemProperties = systemProperties;

        this.serviceMap = new HashMap<>();

        // todo: remove This.
        contractExecutor = new ContractExecutor(frameworkLauncher.getFramework(), contractStore, systemProperties, logStore);

        this.frameworkHashMap.put(frameworkLauncher.getBranchId(), frameworkLauncher);
        this.bundleService = bundleService;
        this.defaultConfig = defaultConfig;
        this.genesis = genesis;

        initBootBundles();

        setDefaultPermission(bootBranchId);

    }

    private void initBootBundles() {

        List<BranchContract> branchContractList = this.getContractList();

        if (branchContractList.isEmpty()) {
            log.warn("This branch {} has no any contract.", bootBranchId);
            return;
        }

        for (BranchContract branchContract : branchContractList) {
            ContractVersion contractVersion = branchContract.getContractVersion();
            // todo : file exist check & download & file validate

            BundleContext context = frameworkHashMap.get(bootBranchId).getBundleContext();
            Bundle bundle = null;

            try {
                bundle = installTest(bootBranchId, contractVersion, branchContract.isSystem());
            } catch (IOException e) {
                log.error("ContractFile has an Error with {}", e.getMessage());
            } catch (BundleException e) {
                log.error("ContractFile {} failed to install with {}", contractVersion, e.getMessage());
            }

            assert bundle != null;

            if (bundle != null) {
                try {
                    startTest(bundle);
                } catch (BundleException e) {
                    log.error("Bundle {} failed to start with {}", bundle.getSymbolicName(), e.getMessage());
                    continue;
                }
                registerServiceMap(bootBranchId, contractVersion, bundle);

                try {
                    inject(context, bundle);
                } catch (IllegalAccessException e) {
                    log.error("Bundle {} failed to inject with {}", bundle.getSymbolicName(), e.getMessage());
                }
            }
        }
    }

    private List<BranchContract> getContractList() {
        if (contractStore.getBranchStore().getBranchContacts().isEmpty()) {
            return genesis.getBranch().getBranchContracts();
        }
        return contractStore.getBranchStore().getBranchContacts();
    }

    public void inject(String branchId, ContractVersion contractVersion) throws IllegalAccessException {
        BundleContext context = findBundleContext(branchId);
        Bundle bundle = bundleService.getBundle(context, contractVersion);

        inject(context, bundle);
    }


    private void inject(BundleContext context, Bundle bundle) throws IllegalAccessException {
        ServiceReference<?>[] serviceRefs = bundle.getRegisteredServices();
        if (serviceRefs == null) {
            return;
        }

        boolean isSystemContract = bundle.getLocation()
                .startsWith(SUFFIX_SYSTEM_CONTRACT);

        for (ServiceReference serviceRef : serviceRefs) {
            Object service = context.getService(serviceRef);

            contractExecutor.injectFields(bundle, service, isSystemContract);
        }
    }

    public Bundle getBundle(String branchId, ContractVersion contractVersion) {
        return bundleService.getBundle(findBundleContext(branchId), contractVersion);
    }

    public Bundle[] getBundles(String branchId) {
        // todo : impl to bundle service
        return bundleService.getBundles(findBundleContext(branchId));
    }

    public void addFramework(FrameworkLauncher launcher) {
        this.frameworkHashMap.put(launcher.getBranchId(), launcher);
    }

    private BundleContext findBundleContext(String branchId) {
//        FrameworkLauncher launcher = this.frameworkHashMap.get(branchId);
        // todo : implements framework not found exception.
        return this.frameworkHashMap.get(branchId).getBundleContext();
    }

    public Bundle installTest(String branchId, ContractVersion contractVersion, boolean isSystem) throws IOException, BundleException {
        File contractFile = new File(defaultConfig.getContractPath() + File.separator + contractVersion + ".jar");

        assert contractFile != null;

        Bundle bundle = bundleService.getBundle(findBundleContext(branchId), contractVersion);

        return bundleService.install(findBundleContext(branchId), contractVersion, contractFile, isSystem);

//        try (JarFile jarFile = new JarFile(contractFile)) {
//            if (bundle != null && isInstalledContract(jarFile, bundle)) {
//                log.debug("Already installed bundle {}", contractVersion);
//                return bundle;
//            }
//
//            if (verifyManifest(jarFile.getManifest())) {
//                log.debug("Installing  bundle {}", contractVersion);
//                return bundleService.install(findBundleContext(branchId), contractVersion, contractFile, isSystem);
//            }
//        }
//        return null;
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

    public void registerServiceMap(String branchId, ContractVersion contractVersion, Bundle bundle) {
        BundleContext context = findBundleContext(branchId);
        Object obj = context.getService(bundle.getRegisteredServices()[0]);
        this.serviceMap.put(contractVersion.toString(), obj);
    }

    public void uninstallTest(String branchId, ContractVersion contractVersion) {
        try {
            bundleService.uninstall(findBundleContext(branchId), contractVersion);
        } catch (BundleException e) {
            log.error(e.getMessage());
        }
    }

    public void startTest(Bundle bundle) throws BundleException {
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

    public String getLog(long index) {
        return contractExecutor.getLog(index);
    }

    public List<String> getLogs(long start, long offset) {
        return contractExecutor.getLogs(start, offset);
    }

    public long getCurLogIndex() {
        return contractExecutor.getCurLogIndex();
    }

    public void reloadInject(String branchId) throws IllegalAccessException {
        Bundle[] bundles = findBundleContext(branchId).getBundles();

        for (Bundle bundle : bundles) {
            inject(findBundleContext(branchId), bundle);
        }
    }

    private boolean verifyManifest(Manifest manifest) {
        String manifestVersion = manifest.getMainAttributes().getValue("Bundle-ManifestVersion");
        String bundleSymbolicName = manifest.getMainAttributes().getValue("Bundle-SymbolicName");
        String bundleVersion = manifest.getMainAttributes().getValue("Bundle-Version");
        return verifyManifest(manifestVersion, bundleSymbolicName, bundleVersion);
    }

    @Deprecated
    private boolean verifyManifest(Bundle bundle) {
        String bundleManifestVersion = bundle.getHeaders().get("Bundle-ManifestVersion");
        String bundleSymbolicName = bundle.getHeaders().get("Bundle-SymbolicName");
        String bundleVersion = bundle.getHeaders().get("Bundle-Version");
        return verifyManifest(bundleManifestVersion, bundleSymbolicName, bundleVersion);
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

    @Deprecated
    public List<ContractStatus> searchContracts() {
        List<ContractStatus> result = new ArrayList<>();
        for (Bundle bundle : findBundleContext(bootBranchId).getBundles()) {
            Version v = bundle.getVersion();
            result.add(getContractStatus(bundle));
        }
        return result;
    }

    public List<ContractStatus> searchContracts(String branchId) {
        List<ContractStatus> result = new ArrayList<>();
        Bundle[] bundleList = findBundleContext(branchId).getBundles();
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

    public Object query(String branchId, String contractVersion, String methodName, JsonObject params) {
        Bundle bundle = bundleService.getBundle(findBundleContext(branchId), ContractVersion.of(contractVersion));
        return bundle != null ? contractExecutor.query(contractVersion, serviceMap.get(contractVersion), methodName, params) : null;
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

        File contractFile = new File(this.getContractPath() + File.separator + version + ".jar");
        if (!contractFile.canRead()) {
            contractFile.setReadable(true, false);
        }

        return contractFile.isFile();
    }

    public boolean downloader(ContractVersion version) {

        int bufferSize = 1024;

        try (OutputStream outputStream = new BufferedOutputStream(
                new FileOutputStream(this.contractPath + File.separator + version + ".jar"))) {
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
            return true;
        } catch (IOException e) {
            log.error(e.getMessage());
            if (deleteContractFile(new File(this.contractPath + File.separator + version + ".jar"))) {
                log.debug("Deleting contract file {} success", version);
            }
            return false;
        }

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


    private void setDefaultPermission(String branchId) {
        BundleContext context = frameworkHashMap.get(branchId).getBundleContext();
        String permissionKey = String.format("%s-container-permission", branchId);

        ServiceReference<ConditionalPermissionAdmin> ref =
                context.getServiceReference(ConditionalPermissionAdmin.class);
        ConditionalPermissionAdmin admin = context.getService(ref);
        ConditionalPermissionUpdate update = admin.newConditionalPermissionUpdate();
        List<ConditionalPermissionInfo> infos = update.getConditionalPermissionInfos();

        //Check existence
        if (infos == null) {
            return;
        }

        for (ConditionalPermissionInfo conditionalPermissionInfo : infos) {
            if (conditionalPermissionInfo.getName().equals(permissionKey)) {
                return;
            }
        }

        List<PermissionInfo> defaultPermissions = new ArrayList<>();

        defaultPermissions.add(new PermissionInfo(PropertyPermission.class.getName(),
                "org.osgi.framework", "read"));
        defaultPermissions.add(new PermissionInfo(PropertyPermission.class.getName(),
                "com.fasterxml.jackson.core.util.BufferRecyclers.trackReusableBuffers", "read"));
        defaultPermissions.add(new PermissionInfo(RuntimePermission.class.getName(),
                "*", "accessDeclaredMembers"));
        defaultPermissions.add(new PermissionInfo(ReflectPermission.class.getName(),
                "*", "suppressAccessChecks"));
        defaultPermissions.add(new PermissionInfo(PackagePermission.class.getName(),
                "*", "import,export,exportonly"));
        defaultPermissions.add(new PermissionInfo(CapabilityPermission.class.getName(),
                "osgi.ee", "require"));
        defaultPermissions.add(new PermissionInfo(CapabilityPermission.class.getName(),
                "osgi.native", "require"));
        defaultPermissions.add(new PermissionInfo(ServicePermission.class.getName(),
                "*", "get,register"));
        defaultPermissions.add(new PermissionInfo(BundlePermission.class.getName(),
                "*", "provide,require,host,fragment"));

        infos.add(admin.newConditionalPermissionInfo(
                permissionKey,
                new ConditionInfo[] {
                        new ConditionInfo(BundleLocationCondition.class.getName(), new String[] {"*"})
                },
                defaultPermissions.toArray(new PermissionInfo[defaultPermissions.size()]),
                ConditionalPermissionInfo.ALLOW));

        //Allow file permission to system contract
        // 시스템 컨트렉트 권한
        // Branch State Store 권한추가 - 읽기/쓰기 권한
        // 컨트렉트 폴더 읽기/쓰기 권한
        // TODO 아카식 시스템 폴더 읽기/쓰기 권한

        String stateStorePath = String.format("%s/%s/state", databasePath, branchId);
        String stateStoreFile = String.format("%s/%s/state/*", databasePath, branchId);

        String branchStorePath = String.format("%s/%s/branch", databasePath, branchId);
        String branchStoreFile = String.format("%s/%s/branch/*", databasePath, branchId);
        String allPermission = "read,write,delete";
        String filePermissionName = FilePermission.class.getName();

        List<PermissionInfo> commonPermissions = new ArrayList<>();
        commonPermissions.add(new PermissionInfo(filePermissionName, stateStorePath, "read"));
        commonPermissions.add(new PermissionInfo(filePermissionName, stateStoreFile, allPermission));
        commonPermissions.add(new PermissionInfo(filePermissionName, branchStorePath, "read"));
        commonPermissions.add(new PermissionInfo(filePermissionName, branchStoreFile, "read"));

        List<PermissionInfo> systemPermissions = commonPermissions;
        // Add Branch File Write
        systemPermissions.add(new PermissionInfo(filePermissionName, branchStoreFile, allPermission));
        // Bundle 파일의 위치로 권한을 할당한다.
        // {BID}-container-permission-system-file
        infos.add(admin.newConditionalPermissionInfo(
                String.format("%s-system-file", permissionKey),
                new ConditionInfo[] {new ConditionInfo(BundleLocationCondition.class.getName(),
                        new String[] {String.format("%s/*", SUFFIX_SYSTEM_CONTRACT)})
                },
                systemPermissions.toArray(new PermissionInfo[systemPermissions.size()]),
                ConditionalPermissionInfo.ALLOW));

        //Allow file permission to user contract
        // 사용자 컨트렉트 권한
        // Branch State Store 권한 추가 - 읽기 권한
        // {BID}-container-permission-user-file
        List<PermissionInfo> userPermissions = commonPermissions;
        userPermissions.add(new PermissionInfo(filePermissionName, branchStoreFile, "read"));

        infos.add(admin.newConditionalPermissionInfo(
                String.format("%s-user-file", permissionKey),
                new ConditionInfo[] {new ConditionInfo(BundleLocationCondition.class.getName(),
                        new String[] {String.format("%s/*", SUFFIX_USER_CONTRACT)})
                },
                userPermissions.toArray(new PermissionInfo[userPermissions.size()]),
                ConditionalPermissionInfo.ALLOW));

        boolean isSuccess = update.commit();
        log.info("Load complete policy: branchID - {}, isSuccess - {}", branchId, isSuccess);
    }

    public Map<String, Object> getServiceMap() {
        return this.serviceMap;
    }
}
