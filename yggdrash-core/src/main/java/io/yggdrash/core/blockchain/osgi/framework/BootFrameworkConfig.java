package io.yggdrash.core.blockchain.osgi.framework;

import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.BranchId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Permission;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;

public class BootFrameworkConfig implements FrameworkConfig {
    private static final Logger log = LoggerFactory.getLogger(BootFrameworkConfig.class);

    private static final String EXTRA_PACKAGES = "io.yggdrash.common.contract"
            + ",io.yggdrash.common.contract.method"
            + ",io.yggdrash.common.contract.standard"
            + ",io.yggdrash.common.contract.vo"
            + ",io.yggdrash.common.rlp"
            + ",io.yggdrash.common.contract.vo.dpoa"
            + ",io.yggdrash.common.contract.vo.dpoa.tx"
            + ",io.yggdrash.common.crypto"
            + ",io.yggdrash.common.crypto.jce"
            + ",io.yggdrash.common.exception"
            + ",io.yggdrash.common.store"
            + ",io.yggdrash.common.store.datasource"
            + ",io.yggdrash.common.utils"
            + ",org.osgi.util.tracker"
            + ",com.google.gson"
            + ",com.google.common.base"
            + ",com.google.common.primitives"
            + ",org.w3c.dom"
            + ",org.slf4j"
            + ",java.math"
            + ",io.yggdrash.contract.core"
            + ",io.yggdrash.contract.core.annotation"
            + ",io.yggdrash.contract.core.exception"
            + ",io.yggdrash.contract.core.store"
            + ",io.yggdrash.contract.core.channel"
            ;



    private Map<String, String> config;

    public BootFrameworkConfig(DefaultConfig defaultConfig, BranchId branchId) {

        Policy.setPolicy(new Policy() {
            @Override
            public boolean implies(ProtectionDomain domain, Permission permission) {
                return true;
            }
        });


        this.config = new HashMap<>();
        this.config.put("org.osgi.framework.system.packages.extra", EXTRA_PACKAGES);
        this.config.put("org.osgi.framework.security", "osgi");

        String frameworkRootPath = String.format("%s/%s", defaultConfig.getOsgiPath(), branchId);
        log.debug("frameworkRootPath : {}", frameworkRootPath);
        if (System.getSecurityManager() != null) {
            this.remove("org.osgi.framework.security");
        }
        this.addOption("org.osgi.framework.storage", frameworkRootPath);
    }

    @Override
    public void addOption(String key, String value) {
        this.config.put(key, value);
    }

    @Override
    public void remove(String key) {
        this.config.remove(key);
    }

    @Override
    public Map<String, String> getConfig() {
        return this.config;
    }
}
