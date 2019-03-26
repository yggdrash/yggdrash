package io.yggdrash.core.blockchain.osgi;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.contract.core.store.OutputStore;
import io.yggdrash.contract.core.store.OutputType;
import io.yggdrash.core.blockchain.SystemProperties;
import io.yggdrash.core.store.StoreContainer;
import io.yggdrash.core.store.TransactionReceiptStore;
import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundlePermission;
import org.osgi.framework.CapabilityPermission;
import org.osgi.framework.PackagePermission;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
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
import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.lang.reflect.ReflectPermission;
import java.net.SocketPermission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PropertyPermission;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class ContractContainer {
    private static final Logger log = LoggerFactory.getLogger(ContractContainer.class);

    public static final String SUFFIX_SYSTEM_CONTRACT = "contract/system";
    public static final String SUFFIX_USER_CONTRACT = "contract/user";

    private Framework framework;

    private final FrameworkFactory frameworkFactory;
    private final Map<String, String> commonContainerConfig;
    private final String branchId;
    private final StoreContainer storeContainer;
    private final DefaultConfig config;
    private final SystemProperties systemProperties;

    private ContractManager contractManager;
    private Map<OutputType, OutputStore> outputStore;

    ContractContainer(FrameworkFactory frameworkFactory, Map<String, String> containerConfig,
                      String branchId, StoreContainer storeContainer, DefaultConfig config,
                      SystemProperties systemProperties, Map<OutputType, OutputStore> outputStore) {
        this.frameworkFactory = frameworkFactory;
        this.commonContainerConfig = containerConfig;
        this.branchId = branchId;
        this.storeContainer = storeContainer;
        this.config = config;
        this.systemProperties = systemProperties;
        this.outputStore = outputStore;
    }

    void newFramework() {
        String containerPath = String.format("%s/%s", config.getOsgiPath(), branchId);
        log.debug("Container Path : {}", containerPath);
        Map<String, String> containerConfig = new HashMap<>();
        containerConfig.put("org.osgi.framework.storage", containerPath);
        containerConfig.putAll(commonContainerConfig);
        if (System.getSecurityManager() != null) {
            containerConfig.remove("org.osgi.framework.security");
        }

        framework = frameworkFactory.newFramework(containerConfig);

        contractManager = new ContractManager(framework, branchId, storeContainer, outputStore, systemProperties);

        try {
            framework.start();
            setDefaultPermission(branchId);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Load contract container exception: branchID - {}, msg - {}", branchId, e.getMessage());
            throw new IllegalStateException(e.getCause());
        }

        log.info("Load contract container: branchID - {}", branchId);
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
        if (infos != null) {
            for (ConditionalPermissionInfo conditionalPermissionInfo : infos) {
                if (conditionalPermissionInfo.getName().equals(permissionKey)) {
                    return;
                }
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
                new ConditionInfo[]{
                        new ConditionInfo(BundleLocationCondition.class.getName(), new String[]{"*"})
                },
                defaultPermissions.toArray(new PermissionInfo[defaultPermissions.size()]),
                ConditionalPermissionInfo.ALLOW));

        //Allow file permission to system contract
        // 시스템 컨트렉트 권한
        // Branch State Store 권한추가 - 읽기/쓰기 권한
        // 컨트렉트 폴더 읽기/쓰기 권한
        // TODO 아카식 시스템 폴더 읽기/쓰기 권한

        String stateStorePath = String.format("%s/%s/state", config.getDatabasePath(), branchId);
        String stateStoreFile = String.format("%s/%s/state/*", config.getDatabasePath(), branchId);

        String branchStorePath = String.format("%s/%s/branch", config.getDatabasePath(), branchId);
        String branchStoreFile = String.format("%s/%s/branch/*", config.getDatabasePath(), branchId);

        String filePermissionName = FilePermission.class.getName();

        List<PermissionInfo> systemPermissions = new ArrayList<>();
        systemPermissions.add(
                new PermissionInfo(filePermissionName, stateStorePath, "read"));
        systemPermissions.add(
                new PermissionInfo(filePermissionName, stateStoreFile, "read,write,delete"));

        // Add Branch Store Read / Write
        systemPermissions.add(
                new PermissionInfo(filePermissionName, branchStorePath, "read"));
        systemPermissions.add(
                new PermissionInfo(filePermissionName, branchStoreFile, "read,write,delete"));
        if (systemProperties != null && !StringUtils.isEmpty(systemProperties.getEsHost())) {
            systemPermissions.add(new PermissionInfo(
                    SocketPermission.class.getName(), systemProperties.getEsHost(), "connect,resolve"));
        }
        // Bundle 파일의 위치로 권한을 할당한다.
        // {BID}-container-permission-system-file
        infos.add(admin.newConditionalPermissionInfo(
                String.format("%s-system-file", permissionKey),
                new ConditionInfo[]{new ConditionInfo(BundleLocationCondition.class.getName(),
                        new String[]{String.format("%s/*", SUFFIX_SYSTEM_CONTRACT)})
                },
                systemPermissions.toArray(new PermissionInfo[systemPermissions.size()]),
                ConditionalPermissionInfo.ALLOW));

        //Allow file permission to user contract
        // 사용자 컨트렉트 권한
        // Branch State Store 권한 추가 - 읽기 권한
        // {BID}-container-permission-user-file
        List<PermissionInfo> userPermissions = new ArrayList<>();
        userPermissions.add(
                new PermissionInfo(filePermissionName, stateStorePath, "read"));
        userPermissions.add(
                new PermissionInfo(filePermissionName, stateStoreFile, "read,write,delete"));
        // Branch Store Read
        userPermissions.add(
                new PermissionInfo(filePermissionName, branchStorePath, "read"));
        userPermissions.add(
                new PermissionInfo(filePermissionName, branchStoreFile, "read"));

        if (systemProperties != null && !StringUtils.isEmpty(systemProperties.getEsHost())) {
            userPermissions.add(
                    new PermissionInfo(SocketPermission.class.getName(),
                            systemProperties.getEsHost(), "connect,resolve"));
        }
        infos.add(admin.newConditionalPermissionInfo(
                String.format("%s-user-file", permissionKey),
                new ConditionInfo[]{new ConditionInfo(BundleLocationCondition.class.getName(),
                        new String[]{String.format("%s/*", SUFFIX_USER_CONTRACT)})
                },
                userPermissions.toArray(new PermissionInfo[userPermissions.size()]),
                ConditionalPermissionInfo.ALLOW));

        boolean isSuccess = update.commit();
        log.info("Load complete policy: branchID - {}, isSuccess - {}", branchId, isSuccess);
    }

    public long installContract(ContractVersion contract, File contractFile, boolean isSystem) {
        // copy System or UserContract
        try {
            Manifest m = new JarFile(contractFile).getManifest();
            //String symbolicName = m.getAttributes("Bundle-SymbolicName");
            if (contractManager.verifyManifest(m)) {
                String symbolicName = m.getMainAttributes().getValue("Bundle-SymbolicName");
                String version = m.getMainAttributes().getValue("Bundle-Version");
                if (contractManager.checkExistContract(symbolicName, version)) {
                    log.error("Contract SymbolicName and Version exist {}-{}", symbolicName, version);
                    return -1L;
                }
            } else {
                log.error("Contract Manifest is not verify");
                return -1L;
            }
        } catch (IOException e) {
            log.error("Contract file don't Load");
            return -1L;
        }

        return contractManager.install(contract, contractFile, isSystem);
    }

    public ContractManager getContractManager() {
        return contractManager;
    }

    public void reloadInject() throws IllegalAccessException {
        // TODO load After call
        for (Bundle bundle : framework.getBundleContext().getBundles()) {
            contractManager.inject(bundle);
        }
    }

    public String getContractPath() {
        return this.config.getContractPath();
    }


}
