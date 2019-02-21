package io.yggdrash.core.blockchain;

import io.yggdrash.contract.store.StateDB;
import io.yggdrash.contract.store.UserStateDB;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.PropertyPermission;

public class ContractContainer {
    private static final Logger log = LoggerFactory.getLogger(ContractContainer.class);

    private FrameworkFactory frameworkFactory;
    private Map<String, String> containerConfig;

    private String branchId;
    private StateDB stateDB;
    private UserStateDB userStateDB;

    private Framework framework;

    private void newFramework() {
        framework = frameworkFactory.newFramework(containerConfig);

        try {
            framework.start();
            setDefaultPermission(branchId);

//            for (Bundle bundle : framework.getBundleContext().getBundles()) {
//                inject(bundle);
//            }
            Arrays.asList(framework.getBundleContext().getBundles()).forEach(b -> log.info("Bundle: {}", b.getSymbolicName()));
            Arrays.asList(framework.getRegisteredServices()).forEach(s -> log.info("Service reference: {}", s.toString()));
        } catch (Exception e) {
            throw new IllegalStateException(e.getCause());
        }
    }

    private void setDefaultPermission(String branch) {
        BundleContext context = framework.getBundleContext();
        String permissionKey = String.format("%s-allow-bundle-permission", branch);


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
        log.info("Load complete policy {}:{}", branch, isSuccess);
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
        private StateDB stateDB;
        private UserStateDB userStateDB;

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

        public ContractContainerBuilder withStateDB(StateDB stateDB) {
            this.stateDB = stateDB;
            return this;
        }

        public ContractContainerBuilder withUserStateDB(UserStateDB userStateDB) {
            this.userStateDB = userStateDB;
            return this;
        }

        public ContractContainer build() {
            if (this.frameworkFactory == null) {
                throw new IllegalStateException("Must set frameworkFactory");
            }

            if (this.containerConfig == null) {
                throw new IllegalStateException("Must set containerConfig");
            }

            if (this.branchId == null) {
                throw new IllegalStateException("Must set branchId");
            }

            ContractContainer contractContainer = new ContractContainer();
            contractContainer.stateDB = this.stateDB;
            contractContainer.frameworkFactory = this.frameworkFactory;
            contractContainer.containerConfig = this.containerConfig;
            contractContainer.userStateDB = this.userStateDB;
            contractContainer.branchId = this.branchId;

            contractContainer.newFramework();
            return contractContainer;
        }
    }
}
