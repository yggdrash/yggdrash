package io.yggdrash.core.blockchain.osgi;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.common.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ReflectPermission;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PropertyPermission;

public class ContractContainer {
    private static final Logger log = LoggerFactory.getLogger(ContractContainer.class);

    private final String SUFFIX_SYSTEM_CONTRACT = "/system-contracts";
    private final String SUFFIX_USER_CONTRACT = "/user-contracts";

    private Framework framework;
    private String systemContractPath;
    private String userContractPath;

    private FrameworkFactory frameworkFactory;
    private Map<String, String> commonContainerConfig;
    private String branchId;
    private StateStore stateStore;
    private TransactionReceiptStore transactionReceiptStore;

    private ContractManager contractManager;

    ContractContainer(FrameworkFactory frameworkFactory, Map<String, String> containerConfig, String branchId
            , StateStore stateStore, TransactionReceiptStore transactionReceiptStore) {
        this.frameworkFactory = frameworkFactory;
        this.commonContainerConfig = containerConfig;
        this.branchId = branchId;
        this.stateStore = stateStore;
        this.transactionReceiptStore = transactionReceiptStore;
    }

    void newFramework() {
        DefaultConfig config = new DefaultConfig();
        String containerPath = String.format("%s/%s", config.getOsgiPath(), branchId);

        Map<String, String> containerConfig = new HashMap<>();
        containerConfig.put("org.osgi.framework.storage", containerPath);
        containerConfig.putAll(commonContainerConfig);
        if (System.getSecurityManager() != null) {
            containerConfig.remove("org.osgi.framework.security");
        }

        framework = frameworkFactory.newFramework(containerConfig);
        systemContractPath = String.format("%s/bundles%s", containerPath, SUFFIX_SYSTEM_CONTRACT);
        userContractPath = String.format("%s/bundles%s", containerPath, SUFFIX_USER_CONTRACT);
        contractManager = new ContractManager(framework, systemContractPath, userContractPath, branchId, stateStore, transactionReceiptStore);

        try {
            framework.start();
            setDefaultPermission(branchId);
            List<String> copiedContracts = copySystemContractToContractPath();
            loadSystemContract(copiedContracts);
            contractManager.setSystemContracts(copiedContracts);

            for (Bundle bundle : framework.getBundleContext().getBundles()) {
                contractManager.inject(bundle);
            }
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
                        new ConditionInfo(BundleLocationCondition.class.getName(), new String[]{
                                "*"
                        })
                },
                permissionInfos.toArray(new PermissionInfo[permissionInfos.size()]),
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
            for (String contract : contracts) {
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

    private void loadSystemContract(List<String> copiedContracts) {
        for (String copiedContract : copiedContracts) {
            contractManager.install(copiedContract, true);
        }
    }

    public ContractManager getContractManager() {
        return contractManager;
    }
}
