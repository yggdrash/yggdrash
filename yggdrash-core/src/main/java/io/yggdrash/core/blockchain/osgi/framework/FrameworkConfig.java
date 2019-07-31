package io.yggdrash.core.blockchain.osgi.framework;

import java.util.Map;

public interface FrameworkConfig {

    void addOption(String key, String value);

    void remove(String key);

    Map<String, String> getConfig();

}
