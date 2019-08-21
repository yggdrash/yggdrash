package io.yggdrash.core.blockchain.osgi.framework;

import io.yggdrash.common.contract.ContractVersion;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import java.io.File;
import java.io.IOException;

public interface BundleService {

    // Bundle Actions
    Bundle install(ContractVersion contractVersion, File file) throws IOException, BundleException;

    void uninstall(ContractVersion contractVersion) throws BundleException;

    void start(ContractVersion contractVersion) throws BundleException;

    void start(Bundle bundle) throws BundleException;

    void stop(ContractVersion contractVersion) throws BundleException;

    int getBundleState(ContractVersion contractVersion);

    Bundle[] getBundles();

    Bundle getBundle(ContractVersion contractVersion);

    Object getBundleService(Bundle bundle);

}
