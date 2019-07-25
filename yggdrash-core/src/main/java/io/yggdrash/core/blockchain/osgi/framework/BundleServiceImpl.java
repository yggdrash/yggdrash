package io.yggdrash.core.blockchain.osgi.framework;

import io.yggdrash.common.contract.ContractVersion;
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

    private static final String SUFFIX_SYSTEM_CONTRACT = "contract/system";
    private static final String SUFFIX_USER_CONTRACT = "contract/user";

    @Override
    public Bundle install(BundleContext context, ContractVersion contractVersion, File file, boolean isSystem) throws IOException, BundleException {

        String absolute = file.getAbsolutePath();
        assert !absolute.isEmpty();

        try (InputStream fs = new FileInputStream(absolute)) {
            String location = location(isSystem, contractVersion);

            assert !location.isEmpty();

            Bundle bundle = context.installBundle(location, fs);

            assert bundle != null;

            return bundle;
        }
    }

    @Override
    public void uninstall(BundleContext context, ContractVersion contractVersion) throws BundleException {
        Bundle bundle = findBundleByContractVersion(context, contractVersion);
        if (bundle != null) {
            bundle.uninstall();
            return;
        }
        log.warn("not found bundle {} in osgi ", contractVersion);
    }

    @Override
    public void start(BundleContext context, ContractVersion contractVersion) throws BundleException {
        Bundle bundle = findBundleByContractVersion(context, contractVersion);
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
    public void stop(BundleContext context,  ContractVersion contractVersion) throws BundleException {
        Bundle bundle = findBundleByContractVersion(context, contractVersion);
        if (bundle != null) {
            bundle.stop();
            return;
        }
        log.warn("not found bundle contract version {} in osgi ", contractVersion);
    }

    @Override
    public int getBundleState(BundleContext context,  ContractVersion contractVersion) {
        Bundle bundle = findBundleByContractVersion(context, contractVersion);
        if (bundle != null) {
            return bundle.getState();
        }
        log.warn("not found bundle contract version {} in osgi ", contractVersion);
        return -1;
    }

    public Bundle getBundle(BundleContext context, long bundleId) {
        return context.getBundle(bundleId);
    }

    @Override
    public Bundle getBundle(BundleContext context, ContractVersion contractVersion) {
        return findBundleByContractVersion(context, contractVersion);
    }

    @Override
    public Bundle[] getBundles(BundleContext context) {
        return context.getBundles();
    }

    public Bundle getBundle(BundleContext context, String contractVersion) {
        return findBundleByContractVersion(context, contractVersion);
    }

    private String location(boolean isSystme, ContractVersion contractVersion) {
        return isSystme
                ? String.format("%s/%s", SUFFIX_SYSTEM_CONTRACT, contractVersion.toString())
                : String.format("%s/%s", SUFFIX_USER_CONTRACT, contractVersion.toString());
    }

    private Bundle findBundleByContractVersion(BundleContext context, ContractVersion contractVersion) {
        return findBundleByContractVersion(context, contractVersion.toString());
    }

    private Bundle findBundleByContractVersion(BundleContext context, String contractVersion) {
        for (Bundle bundle : context.getBundles()) {
            if (bundle.getLocation().contains(contractVersion)) {
                return bundle;
            }
        }
        return null;
    }

}
