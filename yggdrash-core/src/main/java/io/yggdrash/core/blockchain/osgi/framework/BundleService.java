package io.yggdrash.core.blockchain.osgi.framework;

import io.yggdrash.common.contract.ContractVersion;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import java.io.File;
import java.io.IOException;

public interface BundleService {

    // Bundle Actions
    Bundle install(BundleContext context, ContractVersion contractVersion, File file, boolean isSystem) throws IOException, BundleException;

    void uninstall(BundleContext context, ContractVersion contractVersion) throws BundleException;

    void start(BundleContext context, ContractVersion contractVersion) throws BundleException;

    void start(Bundle bundle) throws BundleException;

    void stop(BundleContext context,  ContractVersion contractVersion) throws BundleException;

    int getBundleState(BundleContext context,  ContractVersion contractVersion);

    Bundle getBundle(BundleContext context, ContractVersion contractVersion);

    Bundle getBundle(BundleContext context, long bundleId);

    Bundle getBundle(BundleContext context, String contractVersion);

//    void setDefaultPermission(BundleContext context, String branchId);


}
