package io.yggdrash.core.blockchain.osgi.framework;

import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.core.blockchain.osgi.ContractConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

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

        try (InputStream fs = new FileInputStream(file.getAbsolutePath())) {
            return context.installBundle(contractVersion.toString(), fs);
        }
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
    public Bundle getBundle(ContractVersion contractVersion) {
        return context.getBundle(contractVersion.toString());
    }

    @Override
    public Object getBundleService(Bundle bundle) {
        return context.getService(bundle.getRegisteredServices()[0]);
    }

}
