package io.yggdrash.core.blockchain;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.runtime.annotation.ContractStateStore;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PropertyPermission;

public class ContractContainer {
    private static final Logger log = LoggerFactory.getLogger(ContractContainer.class);

    private final String CONTRACT_RESOURCE_PATH = "/system-contracts";
    private final String PREFIX_BUNDLE_PATH = "file:";

    private FrameworkFactory frameworkFactory;
    private Map<String, String> commonContainerConfig;

    private String branchId;
    private StateStore stateStore;
    private TransactionReceiptStore transactionReceiptStore;

    private Framework framework;
    private String bundlePath;

    private void newFramework() {
        DefaultConfig config = new DefaultConfig();
        String containerPath = String.format("%s/%s", config.getOsgiPath(), branchId);
        bundlePath = String.format("%s/bundles", containerPath);

        Map<String, String> containerConfig = new HashMap<>();
        containerConfig.put("org.osgi.framework.storage", containerPath);
        containerConfig.putAll(commonContainerConfig);
        framework = frameworkFactory.newFramework(containerConfig);

        try {
            framework.start();
            setDefaultPermission(branchId);
            List<String> copiedContracts = copySystemContractToContractPath();
            loadSystemContract(copiedContracts);


            for (Bundle bundle : framework.getBundleContext().getBundles()) {
                inject(bundle);
            }
            Arrays.asList(framework.getBundleContext().getBundles()).forEach(b -> log.info("Bundle: {}", b.getSymbolicName()));
            Arrays.asList(framework.getRegisteredServices()).forEach(s -> log.info("Service reference: {}", s.toString()));
        } catch (Exception e) {
            throw new IllegalStateException(e.getCause());
        }
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
        List<String> copiedContracts = new ArrayList<>();
        try {
            //Read system contract files
            try (
                    InputStream in = getClass().getResourceAsStream(CONTRACT_RESOURCE_PATH);
                    BufferedReader br = new BufferedReader(new InputStreamReader(in))
            ) {
                String resource;
                while ((resource = br.readLine()) != null) {
                    contracts.add(String.format("%s/%s", CONTRACT_RESOURCE_PATH, resource));
                }
            }

            //Copy contract
            for (String contract : contracts) {
                URL inputUrl = getClass().getResource(contract);
                File dest = new File(String.format("%s/%s", bundlePath, contract));
                FileUtils.copyURLToFile(inputUrl, dest);
                copiedContracts.add(dest.getPath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return copiedContracts;
    }

    private void loadSystemContract(List<String> copiedContracts) {
        for (String copiedContract : copiedContracts) {
            install(copiedContract);
        }
    }

    private void inject(Bundle bundle) throws IllegalAccessException {
        ServiceReference<?>[] serviceRefs = bundle.getRegisteredServices();
        if (serviceRefs == null) {
            return;
        }

        boolean isSystemContract = bundle.getLocation().startsWith(String.format("%s%s", bundlePath, CONTRACT_RESOURCE_PATH)) ? true : false;

        for (ServiceReference serviceRef : serviceRefs) {
            Object service = framework.getBundleContext().getService(serviceRef);
            injectField(service, service.getClass().getDeclaredFields(), isSystemContract);
        }
    }

    private void injectField(Object o, Field[] fields, boolean isSystemContract) throws IllegalAccessException {
        for (Field field : fields) {
            field.setAccessible(true);
            for (Annotation annotation : field.getDeclaredAnnotations()) {
                if (isSystemContract) {
                    if (annotation.annotationType().equals(ContractStateStore.class)) {
                        field.set(o, stateStore);
                    }
                }
            }
        }
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
                    bundle.start();
                    inject(bundle);
                    break;
                case STOP:
                    bundle.stop();
                    break;
            }
        } catch (Exception e) {
            System.out.println(e.getCause());
            throw new RuntimeException(e);
        }
        return true;
    }

    private boolean verifyManifest(Bundle bundle) {
        String bundleManifestVersion = bundle.getHeaders().get("Bundle-ManifestVersion");
        String bundleSymbolicName = bundle.getHeaders().get("Bundle-SymbolicName");
        String bundleVersion = bundle.getHeaders().get("Bundle-Version");
        if (!"2".equals(bundleManifestVersion)) {
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

    public long install(String contractLocation) {
        Bundle bundle;
        try {
            bundle = framework.getBundleContext().installBundle(String.format("%s%s", PREFIX_BUNDLE_PATH, contractLocation));
            boolean isPass = verifyManifest(bundle);
            if (!isPass) {
                uninstall(bundle.getBundleId());
            }

            start(bundle.getBundleId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return bundle.getBundleId();
    }

    public boolean uninstall(long contractId) {
        return action(contractId, ActionType.UNINSTALL);
    }

    public boolean start(long contractId) {
        return action(contractId, ActionType.START);
    }

    public List<ContractStatus> searchContracts() {
        List<ContractStatus> result = new ArrayList<>();
        for (Bundle bundle : framework.getBundleContext().getBundles()) {
            Version v = bundle.getVersion();
            result.add(new ContractStatus(
                    String.format("%s.%s.%s", v.getMajor(), v.getMinor(), v.getMicro()),
                    bundle.getBundleId(),
                    bundle.getLocation(),
                    bundle.getState()
            ));
        }
        return result;
    }


    public static final class ContractContainerBuilder {
        private FrameworkFactory frameworkFactory;
        private Map<String, String> containerConfig;
        private String branchId;
        private StateStore stateStore;
        private TransactionReceiptStore transactionReceiptStore;

        private ContractContainerBuilder() {
        }

        static ContractContainerBuilder aContractContainer() {
            return new ContractContainerBuilder();
        }

        public ContractContainerBuilder withFrameworkFactory(FrameworkFactory frameworkFactory) {
            this.frameworkFactory = frameworkFactory;
            return this;
        }

        public ContractContainerBuilder withContainerConfig(Map<String, String> containerConfig) {
            this.containerConfig = containerConfig;
            return this;
        }

        public ContractContainerBuilder withBranchId(String branchId) {
            this.branchId = branchId;
            return this;
        }

        public ContractContainerBuilder withStateStore(StateStore stateStore) {
            this.stateStore = stateStore;
            return this;
        }

        public ContractContainerBuilder withTransactionReceiptStore(TransactionReceiptStore transactionReceiptStore) {
            this.transactionReceiptStore = transactionReceiptStore;
            return this;
        }

        public ContractContainer build() {
            if (this.frameworkFactory == null) {
                throw new IllegalStateException("Must set frameworkFactory");
            }

            if (this.containerConfig == null) {
                throw new IllegalStateException("Must set commonContainerConfig");
            }

            if (this.branchId == null) {
                throw new IllegalStateException("Must set branchId");
            }

            ContractContainer contractContainer = new ContractContainer();
            contractContainer.stateStore = this.stateStore;
            contractContainer.transactionReceiptStore = this.transactionReceiptStore;
            contractContainer.frameworkFactory = this.frameworkFactory;
            contractContainer.commonContainerConfig = this.containerConfig;
            contractContainer.branchId = this.branchId;

            contractContainer.newFramework();
            return contractContainer;
        }
    }
}
