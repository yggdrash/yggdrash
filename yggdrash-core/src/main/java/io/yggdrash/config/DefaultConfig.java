package io.yggdrash.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default Configuration Class.
 */
public class DefaultConfig {

    private static Logger logger = LoggerFactory.getLogger("general");

    private final ClassLoader classLoader;

    private Config config;

    public DefaultConfig() {
        this(ConfigFactory.empty());
    }

    public DefaultConfig(Config apiConfig) {
        this(apiConfig, DefaultConfig.class.getClassLoader());
    }


    public DefaultConfig(Config apiConfig, ClassLoader classLoader) {
        try {
            this.classLoader = classLoader;

            Config javaSystemProperties = ConfigFactory.load("no-such-resource-only-system-props");
            Config referenceConfig = ConfigFactory.parseResources("yggdrash.conf");

            config = apiConfig.withFallback(referenceConfig);
            config = javaSystemProperties.withFallback(config).resolve();

        } catch (Exception e) {
            logger.error("Can't read config.");
            throw new RuntimeException(e);
        }

    }

    public Config getConfig() {
        return config;
    }
}
