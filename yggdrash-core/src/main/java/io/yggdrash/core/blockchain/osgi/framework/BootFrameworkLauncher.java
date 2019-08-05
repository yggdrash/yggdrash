package io.yggdrash.core.blockchain.osgi.framework;

import io.yggdrash.core.blockchain.BranchId;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class BootFrameworkLauncher implements FrameworkLauncher {
    private Framework framework;
    private BundleContext context;

    private static final Logger log = LoggerFactory.getLogger(BootFrameworkConfig.class);

    public BootFrameworkLauncher(FrameworkConfig frameworkConfig) {
        launch(frameworkConfig);
    }

    @Override
    public void launch(FrameworkConfig frameworkConfig) {

        this.framework = frameworkFactory.newFramework(frameworkConfig.getConfig());

        try {
            this.framework.start();
        } catch (BundleException e) {
            log.error("{}", e.getMessage());
        }
        this.context = framework.getBundleContext();
    }

    @Override
    public Map<String, String> getConfig() {
        return null;
    }

    @Override
    public BranchId getBranchId() {
        String storageProperty = this.framework.getBundleContext().getProperty("org.osgi.framework.storage");
        return BranchId.of(storageProperty.substring(storageProperty.lastIndexOf('/') + 1));
    }

    @Override
    public BundleContext getBundleContext() {
        return this.context;
    }

    @Override
    public Framework getFramework() {
        return this.framework;
    }

}
