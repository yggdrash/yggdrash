package io.yggdrash.core.blockchain.osgi;

import com.google.gson.JsonObject;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.common.exception.FailedOperationException;
import io.yggdrash.common.utils.JsonUtil;
import io.yggdrash.core.blockchain.SystemProperties;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.consensus.ConsensusBlock;
import io.yggdrash.core.runtime.result.BlockRuntimeResult;
import io.yggdrash.core.runtime.result.TransactionRuntimeResult;
import io.yggdrash.core.store.ContractStore;
import io.yggdrash.core.store.LogStore;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundlePermission;
import org.osgi.framework.CapabilityPermission;
import org.osgi.framework.PackagePermission;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
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

    private Framework framework;

    private final FrameworkFactory frameworkFactory;
    private final Map<String, String> commonContractManagerConfig;
    private final String branchId;
    private final ContractStore contractStore;
    private final LogStore logStore;

    private final String contractRepositoryUrl;
    private final String osgiPath;
    private final String databasePath;
    private final String contractPath;
    private final SystemProperties systemProperties;
    private final Map<String, String> fullLocation; // => Map<contractVersion, fullLocation>
    private final Map<String, Object> serviceMap;

    private ContractExecutor contractExecutor;

    ContractManager(FrameworkFactory frameworkFactory, Map<String, String> contractManagerConfig,
                    String branchId, ContractStore contractStore, String osgiPath, String databasePath,
                    String contractPath, SystemProperties systemProperties, LogStore logStore,
                    String contractRepositoryUrl) {
        this.frameworkFactory = frameworkFactory;
        this.commonContractManagerConfig = contractManagerConfig;
        this.branchId = branchId;
        this.contractStore = contractStore;
        this.logStore = logStore;

        this.osgiPath = osgiPath;
        this.databasePath = databasePath;
        this.contractPath = contractPath;

        this.systemProperties = systemProperties;
        if (contractRepositoryUrl == null) {
            // init null check
            // TODO default contract Repository Url move to config
            contractRepositoryUrl = "http://store.yggdrash.io/contract/";
        }
        this.contractRepositoryUrl = contractRepositoryUrl;
        this.fullLocation = new HashMap<>();
        this.serviceMap = new HashMap<>();
        newFramework();
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

    private String getFullLocation(String contractVersion) {
        return fullLocation.get(contractVersion);
    }

    private void setFullLocation(String location) {
        if (!fullLocation.containsValue(location)) {
            String contractVersion = location.substring(location.lastIndexOf('/') + 1);
            fullLocation.put(contractVersion, location);
        }
    }

    private Object getService(Bundle bundle) {
        //Assume one service
        ServiceReference serviceRef = bundle.getRegisteredServices()[0];
        return framework.getBundleContext().getService(serviceRef);
    }

    private String getContractVersion(Transaction tx) {
        JsonObject txBody = JsonUtil.parseJsonObject(tx.getBody().toString());
        return txBody.get("contractVersion").getAsString();
    }

    private Bundle getBundle(String contractVersion) {
        Object contractBundleLocation = getFullLocation(contractVersion);
        return getBundle(contractBundleLocation);
    }

    private Bundle getBundle(Object identifier) {
        Bundle bundle = null;
        if (identifier instanceof String) {
            bundle = framework.getBundleContext().getBundle((String) identifier);
        } else if (identifier instanceof Long) {
            bundle = framework.getBundleContext().getBundle((long) identifier);
        }
        return bundle;
    }

    private void newFramework() {
        String managerPath = String.format("%s/%s", osgiPath, branchId);
        log.debug("ContractManager Path : {}", managerPath);
        Map<String, String> contractManagerConfig = new HashMap<>();
        contractManagerConfig.put("org.osgi.framework.storage", managerPath);
        contractManagerConfig.putAll(commonContractManagerConfig);
        if (System.getSecurityManager() != null) {
            contractManagerConfig.remove("org.osgi.framework.security");
        }

        framework = frameworkFactory.newFramework(contractManagerConfig);

        contractExecutor = new ContractExecutor(framework, contractStore, systemProperties, logStore);

        try {
            framework.start();
            setDefaultPermission(branchId);
        } catch (Exception e) {
            throw new IllegalStateException(e.getCause());
        }

        log.info("Load contract manager: branchID - {}", branchId);
    }

    private void setDefaultPermission(String branchId) {
        BundleContext context = framework.getBundleContext();
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

    public long installContract(ContractVersion contract, File contractFile, boolean isSystem) {
        long bundleId = -1L;
        try (JarFile jar = new JarFile(contractFile)) {
            Manifest m = jar.getManifest();
            if (m != null && verifyManifest(m)) {
                String symbolicName = m.getMainAttributes().getValue("Bundle-SymbolicName");
                String version = m.getMainAttributes().getValue("Bundle-Version");
                if (checkExistContract(symbolicName, version)) {
                    log.error("Contract SymbolicName and Version exist {}-{}", symbolicName, version);
                    return bundleId;
                }
            } else {
                log.error("Contract Manifest is not verify");
                return bundleId;
            }

        } catch (IOException e) {
            log.error("Contract file don't Load [{}]", e.getMessage()); //TODO Throw Runtime exception


            return bundleId;
        }
        bundleId = install(contract, contractFile, isSystem);
        return bundleId;
    }

    public void reloadInject() throws IllegalAccessException {
        // TODO load After call
        for (Bundle bundle : framework.getBundleContext().getBundles()) {
            setFullLocation(bundle.getLocation()); // Cache the full location of an existing bundle.
            inject(bundle);
        }
    }

    private boolean verifyManifest(Manifest manifest) {
        String manifestVersion = manifest.getMainAttributes().getValue("Bundle-ManifestVersion");
        String bundleSymbolicName = manifest.getMainAttributes().getValue("Bundle-SymbolicName");
        String bundleVersion = manifest.getMainAttributes().getValue("Bundle-Version");
        return verifyManifest(manifestVersion, bundleSymbolicName, bundleVersion);
    }

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

    boolean checkExistContract(String symbol, String version) {
        for (Bundle b : framework.getBundleContext().getBundles()) {
            if (b.getVersion().toString().equals(version) && b.getSymbolicName().equals(symbol)) {
                setFullLocation(b.getLocation()); // Cache the full location of an existing bundle.
                return true;
            }
        }
        return false;
    }

    public List<ContractStatus> searchContracts() {
        List<ContractStatus> result = new ArrayList<>();
        for (Bundle bundle : framework.getBundleContext().getBundles()) {
            Dictionary<String, String> header = bundle.getHeaders();
            int serviceCnt = bundle.getRegisteredServices() == null ? 0 : bundle.getRegisteredServices().length;

            Version v = bundle.getVersion();
            result.add(new ContractStatus(
                    bundle.getSymbolicName(),
                    String.format("%s.%s.%s", v.getMajor(), v.getMinor(), v.getMicro()),
                    header.get("Bundle-Vendor"),
                    header.get("Bundle-Description"),
                    bundle.getBundleId(),
                    bundle.getLocation(),
                    bundle.getState(),
                    serviceCnt
            ));
        }
        return result;
    }

    private long install(ContractVersion version, File file, boolean isSystem) {
        Bundle bundle;
        try (InputStream fileStream = new FileInputStream(file.getAbsoluteFile())) {

            // set location
            String locationPrefix = isSystem ? ContractManager.SUFFIX_SYSTEM_CONTRACT :
                    ContractManager.SUFFIX_USER_CONTRACT;

            String location = String.format("%s/%s", locationPrefix, version.toString());
            // set Location
            bundle = framework.getBundleContext().installBundle(location, fileStream);
            log.debug("installed  {} {}", version, bundle.getLocation());

            boolean isPass = verifyManifest(bundle);
            if (!isPass) {
                uninstall(bundle.getBundleId());
            }
            start(bundle.getBundleId());
            setFullLocation(bundle.getLocation());
            // add Service Map
            serviceMap.put(version.toString(), getService(bundle));

        } catch (Exception e) {
            log.error("Install bundle exception: branchID - {}, msg - {}", branchId, e.getMessage());
            throw new FailedOperationException(e);
        }

        return bundle.getBundleId();
    }

    private boolean uninstall(long contractId) {
        return action(contractId, ActionType.UNINSTALL);
    }

    private boolean start(long contractId) {
        return action(contractId, ActionType.START);
    }

    private boolean stop(long contractId) {
        return action(contractId, ActionType.STOP);
    }

    private enum ActionType {
        UNINSTALL,
        START,
        STOP
    }

    private boolean action(Object identifier, ActionType action) {
        Bundle bundle = getBundle(identifier);
        if (bundle == null) {
            return false;
        }

        try {
            switch (action) {
                case UNINSTALL:
                    bundle.uninstall();
                    break;
                case START:
                    try {
                        bundle.start();
                        // ContractStore is null in Test
                        if (contractStore != null) {
                            inject(bundle);
                        }
                    } catch (Exception e) {
                        bundle.uninstall();
                        throw new FailedOperationException(e);
                    }
                    break;
                case STOP:
                    bundle.stop();
                    break;
                default:
                    throw new FailedOperationException("Action is not Exist");
            }
        } catch (BundleException e) {

            log.error("Execute bundle exception: contractId:{}, path:{}, stack:{}",
                    bundle.getBundleId(), bundle.getLocation(), ExceptionUtils.getStackTrace(e));
            throw new FailedOperationException(e);
        }
        return true;
    }

    private void inject(Bundle bundle) throws IllegalAccessException {
        ServiceReference<?>[] serviceRefs = bundle.getRegisteredServices();
        if (serviceRefs == null) {
            return;
        }

        boolean isSystemContract = bundle.getLocation()
                .startsWith(SUFFIX_SYSTEM_CONTRACT);

        for (ServiceReference serviceRef : serviceRefs) {
            Object service = framework.getBundleContext().getService(serviceRef);

            contractExecutor.injectFields(bundle, service, isSystemContract);
        }
    }

    public Object query(String contractVersion, String methodName, JsonObject params) {
        Bundle bundle = getBundle(contractVersion);
        return bundle != null ? contractExecutor.query(contractVersion, getService(bundle), methodName, params) : null;
    }

    public BlockRuntimeResult executeTxs(ConsensusBlock nextBlock) {
        nextBlock.getBody().getTransactionList().forEach(tx -> registerContract(getContractVersion(tx)));
        return contractExecutor.executeTxs(serviceMap, nextBlock);
    }

    public TransactionRuntimeResult executeTx(Transaction tx) {
        registerContract(getContractVersion(tx));
        return contractExecutor.executeTx(serviceMap, tx);
    }

    private void registerContract(String contractVersion) { // Register contractVersion and service to serviceMap
        Bundle bundle = getBundle(contractVersion);
        if (!serviceMap.containsKey(contractVersion)) { // Check the version exists in serviceMap
            if (bundle == null) {
                serviceMap.put(contractVersion, null);
                log.debug("[Contract not registered] : {} (Bundle is null)", contractVersion);
            } else {
                serviceMap.put(contractVersion, getService(bundle));
                log.debug("[Contract registered] : {}", contractVersion);
            }
        }
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
        }  catch (IOException e) {
            log.error(e.getMessage());
            return false;
        }
    }

    public boolean deleteContractFile(File contractFile) {
        return contractFile.delete();
    }

}
