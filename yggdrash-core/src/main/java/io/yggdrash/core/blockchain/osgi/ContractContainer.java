package io.yggdrash.core.blockchain.osgi;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.core.blockchain.BranchContract;
import io.yggdrash.core.blockchain.SystemProperties;
import io.yggdrash.core.store.TransactionReceiptStore;
import io.yggdrash.contract.core.store.OutputStore;
import io.yggdrash.contract.core.store.OutputType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
import java.io.InputStream;
import java.lang.reflect.ReflectPermission;
import java.net.SocketPermission;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PropertyPermission;

public class ContractContainer {
    private static final Logger log = LoggerFactory.getLogger(ContractContainer.class);

    static final String PREFIX_BUNDLE_PATH = "file:";
    private static final String SUFFIX_SYSTEM_CONTRACT = "/system-contracts";
    private static final String SUFFIX_USER_CONTRACT = "/user-contracts";

    private Framework framework;
    private String systemContractPath;
    private String userContractPath;

    private final FrameworkFactory frameworkFactory;
    private final Map<String, String> commonContainerConfig;
    private final String branchId;
    private final StateStore stateStore;
    private final TransactionReceiptStore transactionReceiptStore;
    private final DefaultConfig config;
    private final SystemProperties systemProperties;

    private ContractManager contractManager;
    private Map<OutputType, OutputStore> outputStore;

    ContractContainer(FrameworkFactory frameworkFactory, Map<String, String> containerConfig,
                      String branchId, StateStore stateStore,
                      TransactionReceiptStore transactionReceiptStore, DefaultConfig config,
                      SystemProperties systemProperties, Map<OutputType, OutputStore> outputStore
            ) {
        this.frameworkFactory = frameworkFactory;
        this.commonContainerConfig = containerConfig;
        this.branchId = branchId;
        this.stateStore = stateStore;
        this.transactionReceiptStore = transactionReceiptStore;
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

        // TOOD remove all file method

        systemContractPath = String.format("%s/bundles%s", containerPath, SUFFIX_SYSTEM_CONTRACT);
        log.debug("systemContractPath Path : {}", systemContractPath);

        userContractPath = String.format("%s/bundles%s", containerPath, SUFFIX_USER_CONTRACT);
        log.debug("userContractPath Path : {}", userContractPath);
        contractManager = new ContractManager(framework, systemContractPath, userContractPath,
                branchId, stateStore, transactionReceiptStore, outputStore, systemProperties);

        try {
            framework.start();
            setDefaultPermission(branchId);
            // TODO Change System contract
//            List<String> copiedContracts = copySystemContractToContractPath();
            //branchContracts.stream().filter(c -> {c.get})


            // TODO Load User Contracts
//            loadSystemContract(copiedContracts);
//            contractManager.setSystemContracts(copiedContracts);

//            for (Bundle bundle : framework.getBundleContext().getBundles()) {
//                contractManager.inject(bundle);
//            }
//            Arrays.asList(framework.getBundleContext().getBundles()).forEach(b -> log.info("Bundle: {}", b.getSymbolicName()));
//            Arrays.asList(framework.getRegisteredServices()).forEach(s -> log.info("Service reference: {}", s.toString()));
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Load contract container exception: branchID - {}, msg - {}", branchId, e.getMessage());
            throw new IllegalStateException(e.getCause());
        }

        log.info("✨Load contract container: branchID - {}", branchId);
    }

    private void setDefaultPermission(String branchId) {
        BundleContext context = framework.getBundleContext();
        String permissionKey = String.format("%s-container-permission", branchId);


        ServiceReference<ConditionalPermissionAdmin> ref = context.getServiceReference(ConditionalPermissionAdmin.class);
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

        List<PermissionInfo> permissionInfos = new ArrayList<>();

        permissionInfos.add(new PermissionInfo(PropertyPermission.class.getName(), "org.osgi.framework", "read"));
        permissionInfos.add(new PermissionInfo(PropertyPermission.class.getName(), "com.fasterxml.jackson.core.util.BufferRecyclers.trackReusableBuffers", "read"));
        permissionInfos.add(new PermissionInfo(RuntimePermission.class.getName(), "*", "accessDeclaredMembers"));

        permissionInfos.add(new PermissionInfo(ReflectPermission.class.getName(), "*", "suppressAccessChecks"));

        permissionInfos.add(new PermissionInfo(PackagePermission.class.getName(), "*", "import,export,exportonly"));
        permissionInfos.add(new PermissionInfo(CapabilityPermission.class.getName(), "osgi.ee", "require"));
        permissionInfos.add(new PermissionInfo(CapabilityPermission.class.getName(), "osgi.native", "require"));
        permissionInfos.add(new PermissionInfo(ServicePermission.class.getName(), "*", "get,register"));
        permissionInfos.add(new PermissionInfo(BundlePermission.class.getName(), "*", "provide,require,host,fragment"));

        infos.add(admin.newConditionalPermissionInfo(
                permissionKey,
                new ConditionInfo[]{
                        new ConditionInfo(BundleLocationCondition.class.getName(), new String[]{"*"})
                },
                permissionInfos.toArray(new PermissionInfo[permissionInfos.size()]),
                ConditionalPermissionInfo.ALLOW));

        //Allow file permission to system contract
        List<PermissionInfo> systemPermissions = new ArrayList<>();
        systemPermissions.add(new PermissionInfo(FilePermission.class.getName(), String.format("%s/%s/state", config.getDatabasePath(), branchId), "read"));
        systemPermissions.add(new PermissionInfo(FilePermission.class.getName(), String.format("%s/%s/state/*", config.getDatabasePath(), branchId), "read,write,delete"));
        if (systemProperties != null && !StringUtils.isEmpty(systemProperties.getEsHost())) {
            systemPermissions.add(new PermissionInfo(SocketPermission.class.getName(), systemProperties.getEsHost(), "connect,resolve"));
        }
        infos.add(admin.newConditionalPermissionInfo(
                String.format("%s-system-file", permissionKey),
                new ConditionInfo[]{new ConditionInfo(BundleLocationCondition.class.getName()
                        , new String[]{String.format("file:%s/*", systemContractPath)})
                },
                systemPermissions.toArray(new PermissionInfo[systemPermissions.size()]),
                ConditionalPermissionInfo.ALLOW));

        //Allow file permission to user contract
        List<PermissionInfo> userPermissions = new ArrayList<>();
        userPermissions.add(new PermissionInfo(FilePermission.class.getName(), String.format("%s/%s/state", config.getDatabasePath(), branchId), "read"));
        userPermissions.add(new PermissionInfo(FilePermission.class.getName(), String.format("%s/%s/state/*", config.getDatabasePath(), branchId), "read,write,delete"));
        if (systemProperties != null && !StringUtils.isEmpty(systemProperties.getEsHost())) {
            userPermissions.add(new PermissionInfo(SocketPermission.class.getName(), systemProperties.getEsHost(), "connect,resolve"));
        }
        infos.add(admin.newConditionalPermissionInfo(
                String.format("%s-user-file", permissionKey),
                new ConditionInfo[]{new ConditionInfo(BundleLocationCondition.class.getName()
                        , new String[]{String.format("file:%s/*", userContractPath)})
                },
                userPermissions.toArray(new PermissionInfo[userPermissions.size()]),
                ConditionalPermissionInfo.ALLOW));

        boolean isSuccess = update.commit();
        log.info("Load complete policy: branchID - {}, isSuccess - {}", branchId, isSuccess);
    }

    private List<String> copySystemContractToContractPath() {
        List<String> contracts = new ArrayList<>();

        InputStream in = null;
        try {
            //Read system contract files
            in = Thread.currentThread().getContextClassLoader().getResourceAsStream(String.format("%s/contracts", SUFFIX_SYSTEM_CONTRACT));
            in = in == null ? getClass().getResourceAsStream(String.format("%s/contracts", SUFFIX_SYSTEM_CONTRACT)) : in;
            if (in == null) {
                return contracts;
            }
            contracts = IOUtils.readLines(in, StandardCharsets.UTF_8);

            //Copy contract
            for (int i = contracts.size() - 1; i >= 0; i--) {
                String contract = contracts.get(i);
                URL inputUrl = getClass().getResource(String.format("%s/%s", SUFFIX_SYSTEM_CONTRACT, contract));
                File dest = new File(contractManager.makeContractPath(contract, true));
                if (dest.exists()) {
                    dest.delete();
                }
                FileUtils.copyURLToFile(inputUrl, dest);
            }
        } catch (IOException e) {
            log.error("Copy system contract exception: branchID - {}, msg - {}", branchId, e.getMessage());
            throw new RuntimeException(e.getCause());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }

        return contracts;
    }

    // TODO remove function
    private void loadSystemContract(List<String> copiedContracts) {
        for (String copiedContract : copiedContracts) {
            contractManager.install(copiedContract, true);
        }
    }

    // TODO remove function
    public void loadUserContract(List<String> userContracts) {
        for(String contract : userContracts) {
            contractManager.install(contract, false);
        }
    }

    public long installContract(ContractVersion contract, File contractFile, boolean isSystem) {
        return contractManager.install(contract, contractFile, isSystem);
    }


    public void copyUserContract(List<BranchContract> contracts) {
        contracts.stream().forEach(c -> {
            URL inputUrl = getClass().getResource(
                    String.format("%s/%s.jar", config.getContractPath(), c.getContractVersion()));
            // Check contract file verify
            File destination = new File(
                    contractManager.makeContractPath(c.getContractVersion()+".jar", false));
            // TODO check File Version verify
            if (!destination.exists()) {
                try {
                    FileUtils.copyURLToFile(inputUrl, destination);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public ContractManager getContractManager() {
        return contractManager;
    }

    public void reloadInject() throws IllegalAccessException {
        for (Bundle bundle : framework.getBundleContext().getBundles()) {
            contractManager.inject(bundle);
        }
    }
}
