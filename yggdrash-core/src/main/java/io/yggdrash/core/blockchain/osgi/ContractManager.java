package io.yggdrash.core.blockchain.osgi;

import io.yggdrash.core.runtime.annotation.ContractStateStore;
import io.yggdrash.core.store.StateStore;
import io.yggdrash.core.store.TransactionReceiptStore;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.launch.Framework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

public class ContractManager {
    private static final Logger log = LoggerFactory.getLogger(ContractManager.class);

    private final String PREFIX_BUNDLE_PATH = "file:";

    private Framework framework;
    private String systemContractPath;
    private String userContractPath;
    private String branchId;
    private StateStore stateStore;
    private TransactionReceiptStore transactionReceiptStore;

    ContractManager(Framework framework, String systemContractPath, String userContractPath, String branchId, StateStore stateStore, TransactionReceiptStore transactionReceiptStore) {
        this.framework = framework;
        this.systemContractPath = systemContractPath;
        this.userContractPath = userContractPath;
        this.branchId = branchId;
        this.stateStore = stateStore;
        this.transactionReceiptStore = transactionReceiptStore;
    }

    String makeContractPath(String contractName, boolean isSystemContract) {
        return String.format("%s/%s", isSystemContract ? systemContractPath : userContractPath, contractName);
    }

    String makeContractFullPath(String contractName, boolean isSystemContract) {
        return String.format("%s%s/%s", PREFIX_BUNDLE_PATH, isSystemContract ? systemContractPath : userContractPath, contractName);
    }

    void inject(Bundle bundle) throws IllegalAccessException {
        ServiceReference<?>[] serviceRefs = bundle.getRegisteredServices();
        if (serviceRefs == null) {
            return;
        }

        boolean isSystemContract = bundle.getLocation().startsWith(String.format("%s%s", PREFIX_BUNDLE_PATH, systemContractPath)) ? true : false;

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

    public long install(String contractLocation, boolean isSystemContract) {
        Bundle bundle;
        try {
            bundle = framework.getBundleContext().installBundle(makeContractFullPath(contractLocation, isSystemContract));
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
}
