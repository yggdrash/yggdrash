package io.yggdrash.core.blockchain.osgi;

import org.osgi.framework.launch.FrameworkFactory;

import java.security.Permission;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

public class ContractPolicyLoader {
    private final String EXTRA_PACKAGES = "com.google.gson" +
            ",io.yggdrash.common.util" +
            ",io.yggdrash.contract.utils" +
            ",io.yggdrash.core.blockchain" +
            ",io.yggdrash.core.contract" +
            ",io.yggdrash.core.runtime.annotation" +
            ",io.yggdrash.core.store" +
            ",io.yggdrash.core.blockchain.dpoa" +
            ",io.yggdrash.contract.annotation" +
            ",io.yggdrash.contract.store" +
            ",io.yggdrash.contract.utils";

    private FrameworkFactory frameworkFactory;
    private Map<String, String> containerConfig;

    public ContractPolicyLoader() {
        // Allow all
        Policy.setPolicy(new Policy() {
            @Override
            public boolean implies(ProtectionDomain domain, Permission permission) {
                return true;
            }
        });

        final Iterator<FrameworkFactory> iterator = ServiceLoader.load(FrameworkFactory.class).iterator();
        if (!iterator.hasNext()) {
            throw new IllegalStateException("Unable to locate OSGi framework factory");
        }
        frameworkFactory = iterator.next();

        containerConfig = new HashMap<>();
        containerConfig.put("org.osgi.framework.system.packages.extra", EXTRA_PACKAGES);
        containerConfig.put("org.osgi.framework.security", "osgi");
    }

    public FrameworkFactory getFrameworkFactory() {
        return frameworkFactory;
    }

    public Map<String, String> getContainerConfig() {
        return containerConfig;
    }
}
