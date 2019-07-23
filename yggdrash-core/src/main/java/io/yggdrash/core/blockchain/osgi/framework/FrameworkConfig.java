package io.yggdrash.core.blockchain.osgi.framework;

import java.util.HashMap;

public interface FrameworkConfig {

    void addOption(String key, String value);

    void remove(String key);

    HashMap<String, String> getConfig();

}
