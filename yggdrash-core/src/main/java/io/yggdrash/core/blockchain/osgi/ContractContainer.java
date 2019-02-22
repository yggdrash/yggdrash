package io.yggdrash.core.blockchain.osgi;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import org.apache.commons.io.FileUtils;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PropertyPermission;

public class ContractContainer {
    private static final Logger log = LoggerFactory.getLogger(ContractContainer.class);

    private final String SUFFIX_SYSTEM_CONTRACT = "/system-contracts";
    private final String PREFIX_BUNDLE_PATH = "file:";

    private Framework framework;
    private String systemContractPath;
    private String userContractPath;

    private FrameworkFactory frameworkFactory;
    private Map<String, String> commonContainerConfig;
    private String branchId;
    private StateStore stateStore;
    private TransactionReceiptStore transactionReceiptStore;

    private ContractManager contractManager;

    private ContractContainer() {

    }

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

        framework = frameworkFactory.newFramework(containerConfig);
        systemContractPath = String.format("%s/bundles%s", containerPath, SUFFIX_SYSTEM_CONTRACT);
        userContractPath = String.format("%s/bundles/user-contracts", containerPath);
        contractManager = new ContractManager(framework, systemContractPath, userContractPath, branchId, stateStore, transactionReceiptStore);
        try {
            framework.start();
            setDefaultPermission(branchId);
            List<String> copiedContracts = copySystemContractToContractPath();
            loadSystemContract(copiedContracts);


            for (Bundle bundle : framework.getBundleContext().getBundles()) {
                contractManager.inject(bundle);
            }
            Arrays.asList(framework.getBundleContext().getBundles()).forEach(b -> log.info("Bundle: {}", b.getSymbolicName()));
            Arrays.asList(framework.getRegisteredServices()).forEach(s -> log.info("Service reference: {}", s.toString()));
        } catch (Exception e) {
            throw new IllegalStateException(e.getCause());
        }

        log.info("✨Load contract container: branchID - {}", branchId);
    }

    private void setDefaultPermission(String branchId) {
        BundleContext context = framework.getBundleContext();
        String permissionKey = String.format("%s-allow-bundle-permission", branchId);


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
        permissionInfos.add(new PermissionInfo(PackagePermission.class.getName(), "*", "import,export,exportonly,import"));
        permissionInfos.add(new PermissionInfo(CapabilityPermission.class.getName(), "osgi.ee", "require"));
        permissionInfos.add(new PermissionInfo(CapabilityPermission.class.getName(), "osgi.native", "require"));
        permissionInfos.add(new PermissionInfo(ServicePermission.class.getName(), "*", "get,register"));
        permissionInfos.add(new PermissionInfo(BundlePermission.class.getName(), "*", "provide,require,host,fragment"));
//        permissionInfos.add(new PermissionInfo(AllPermission.class.getName(), "*", "*"));
//        if (adminPermission) {
//            permissionInfos.add(new PermissionInfo(FilePermission.class.getName(), "<<ALL FILES>>", "read,write,delete"));
//        }

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
        log.info("Load complete policy {}:{}", branchId, isSuccess);
    }

    private List<String> copySystemContractToContractPath() {
        List<String> contracts = new ArrayList<>();
        try {
            //Read system contract files
            try (
                    InputStream in = getClass().getResourceAsStream(SUFFIX_SYSTEM_CONTRACT);
                    BufferedReader br = new BufferedReader(new InputStreamReader(in))
            ) {
                String resource;
                while ((resource = br.readLine()) != null) {
                    contracts.add(resource);
                }
            }

            //Copy contract
            for (String contract : contracts) {
                URL inputUrl = getClass().getResource(String.format("%s/%s", SUFFIX_SYSTEM_CONTRACT, contract));
                File dest = new File(contractManager.makeContractPath(contract, true));
                FileUtils.copyURLToFile(inputUrl, dest);
            }
        } catch (IOException e) {
            e.printStackTrace();
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
