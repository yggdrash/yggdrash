package io.yggdrash.core.blockchain.osgi.framework;

import org.osgi.framework.BundleContext;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import java.util.Map;
import java.util.ServiceLoader;

public interface FrameworkLauncher {

    FrameworkFactory frameworkFactory = ServiceLoader.load(FrameworkFactory.class).iterator().next();

    void launch(FrameworkConfig frameworkConfig);

    Map<String, String> getConfig();

    String getBranchId();

    BundleContext getBundleContext();

    Framework getFramework();

}
