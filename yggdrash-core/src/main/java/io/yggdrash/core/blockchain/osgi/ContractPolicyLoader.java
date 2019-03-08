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
    private final String EXTRA_PACKAGES = "io.yggdrash.common.contract" +
            ",io.yggdrash.common.contract.method" +
            ",io.yggdrash.common.contract.vo" +
            ",io.yggdrash.common.contract.vo.dpoa" +
            ",io.yggdrash.common.contract.vo.dpoa.tx" +
            ",io.yggdrash.common.branch" +
            ",io.yggdrash.common.crypto" +
            ",io.yggdrash.common.crypto.jce" +
            ",io.yggdrash.common.exception" +
            ",io.yggdrash.common.store" +
            ",io.yggdrash.common.store.datasource" +
            ",io.yggdrash.common.utils" +

            ",org.osgi.util.tracker" +
            ",com.google.gson" +
            ",org.w3c.dom" +
            ",org.slf4j" +
            ",java.math" +
            ",io.yggdrash.contract.core" +
            ",io.yggdrash.contract.core.annotation" +
            ",io.yggdrash.contract.core.store";

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
