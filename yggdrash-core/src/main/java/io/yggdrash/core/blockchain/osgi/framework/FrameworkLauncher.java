package io.yggdrash.core.blockchain.osgi.framework;

import org.osgi.framework.BundleContext;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import java.util.HashMap;
import java.util.ServiceLoader;

public interface FrameworkLauncher {

    FrameworkFactory frameworkFactory = ServiceLoader.load(FrameworkFactory.class).iterator().next();

    void launch(FrameworkConfig frameworkConfig);

    HashMap<String, String> getConfig();

    String getBranchId();

    BundleContext getBundleContext();

    Framework getFramework();

}
