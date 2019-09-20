package io.yggdrash.core.blockchain.osgi.framework;

import io.yggdrash.common.contract.ContractVersion;
import io.yggdrash.core.blockchain.osgi.ContractStatus;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface BundleService {

    // Bundle Actions
    Bundle install(ContractVersion contractVersion, File file) throws IOException, BundleException;

    void uninstall(ContractVersion contractVersion) throws BundleException;

    void start(ContractVersion contractVersion) throws BundleException;

    void start(Bundle bundle) throws BundleException;

    void stop(ContractVersion contractVersion) throws BundleException;

    int getBundleState(ContractVersion contractVersion);

    Bundle[] getBundles();

    List<Bundle> getBundlesByName(String contractName);

    Bundle getBundle(ContractVersion contractVersion);

    Object getBundleService(Bundle bundle);

    List<ContractStatus> getContractList();

}
