package io.yggdrash.core.blockchain.osgi.framework;

import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.core.blockchain.osgi.ContractConstants;
import io.yggdrash.core.blockchain.osgi.ContractStatus;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class BundleServiceImpl implements BundleService {
    private static final Logger log = LoggerFactory.getLogger(BundleServiceImpl.class);

    private final BundleContext context;

    public BundleServiceImpl(BundleContext context) {
        this.context = context;

        // todo : remove this code when bundle location prefix all removed.
        uninstallPrefixedBundle();
    }

    private void uninstallPrefixedBundle() {
        Bundle[] bundles = getBundles();

        // bundles[0] is System Bundle
        for (int i = 1; i < bundles.length; i++) {
            if (bundles[i].getLocation().startsWith(ContractConstants.SUFFIX_SYSTEM_CONTRACT)) {
                try {
                    log.debug("remove old bundle {}", bundles[i].getLocation());
                    bundles[i].uninstall();
                } catch (BundleException e) {
                    log.trace(e.getMessage());
                }
            }
        }
    }

    @Override
    public Bundle install(ContractVersion contractVersion, File file) throws IOException, BundleException {

        Bundle bundle = getBundle(contractVersion);

        try (JarFile jarFile = new JarFile(file);
                InputStream fs = new FileInputStream(file.getAbsolutePath())) {
            if (bundle != null && isInstalledContract(jarFile, bundle)) {
                log.warn("Already installed bundle {}", contractVersion);
                return bundle;
            }

            if (verifyManifest(jarFile.getManifest())) {
                log.info("Installing  bundle {}", contractVersion);
                return context.installBundle(contractVersion.toString(), fs);
            }
        }
        return null;
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


    @Override
    public void uninstall(ContractVersion contractVersion) throws BundleException {
        Bundle bundle = getBundle(contractVersion);
        if (bundle != null) {
            bundle.uninstall();
            return;
        }
        log.warn("not found bundle {} in osgi ", contractVersion);
    }

    @Override
    public void start(ContractVersion contractVersion) throws BundleException {
        Bundle bundle = getBundle(contractVersion);
        if (bundle != null) {
            bundle.start();
            return;
        }
        log.warn("not found bundle {} in osgi ", contractVersion);
    }

    @Override
    public void start(Bundle bundle) throws BundleException {
        bundle.start();
    }

    @Override
    public void stop(ContractVersion contractVersion) throws BundleException {
        Bundle bundle = getBundle(contractVersion);
        if (bundle != null) {
            bundle.stop();
            return;
        }
        log.warn("not found bundle contract version {} in osgi ", contractVersion);
    }

    @Override
    public int getBundleState(ContractVersion contractVersion) {
        Bundle bundle = getBundle(contractVersion);
        if (bundle != null) {
            return bundle.getState();
        }
        log.warn("not found bundle contract version {} in osgi ", contractVersion);
        return -1;
    }

    @Override
    public Bundle[] getBundles() {
        return context.getBundles();
    }

    @Override
    public List<Bundle> getBundlesByName(String contractName) {
        List<Bundle> bundleList = new ArrayList<>();

        for (Bundle bundle: getBundles()) {
            Dictionary<String, String> header = bundle.getHeaders();
            if (header.get("Bundle-Name").equals(contractName)) {
                bundleList.add(bundle);
            }
        }

        return bundleList;

    }

    @Override
    public Bundle getBundle(ContractVersion contractVersion) {
        return context.getBundle(contractVersion.toString());
    }

    @Override
    public Object getBundleService(Bundle bundle) {
        return context.getService(bundle.getRegisteredServices()[0]);
    }

    @Override
    public List<ContractStatus> getContractList() {
        List<ContractStatus> contractStatusList = new ArrayList<>();

        for (Bundle bundle: getBundles()) {
            Dictionary<String, String> header = bundle.getHeaders();
            int serviceCnt = bundle.getRegisteredServices() == null ? 0 : bundle.getRegisteredServices().length;

            Version v = bundle.getVersion();
            ContractStatus status = new ContractStatus(
                    bundle.getSymbolicName(),
                    String.format("%s.%s.%s", v.getMajor(), v.getMinor(), v.getMicro()),
                    header.get("Bundle-Vendor"),
                    header.get("Bundle-Description"),
                    bundle.getBundleId(),
                    bundle.getLocation(),
                    bundle.getState(),
                    serviceCnt
            );
            contractStatusList.add(status);
        }

        return contractStatusList;


    }
}
