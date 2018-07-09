package io.yggdrash.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;

import java.util.Map;

import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class DefaultConfigTest {

    /**
     * This is the default config test code.
     * get system config, spring config, yggdrash.conf config.
     */
    @Test
    public void defaultConfigTest() {
        DefaultConfig defaultConfig = new DefaultConfig();
        for (Map.Entry<String, ConfigValue> entry : defaultConfig.getConfig().entrySet()) {
            System.out.println("Name:  " + entry.getKey());
            System.out.println(entry);
        }

        assertThat(defaultConfig.getConfig().getString("java.version"), containsString("1.8"));
        System.out.println("DefaultConfig java.version: " + defaultConfig.getConfig().getString("java.version"));

    }

    /**
     * This is the config test as java version.
     */
    @Test
    public void javaVersionConfigTest() {
        DefaultConfig defaultConfig = new DefaultConfig();

        assertThat(defaultConfig.getConfig().getString("java.version"), containsString("1.8"));

        System.out.println("DefaultConfig java.version: " + defaultConfig.getConfig().getString("java.version"));

    }

    /**
     * This is the config test as java version.
     */
    @Test
    public void yggdrashConfConfigTest() {
        DefaultConfig defaultConfig = new DefaultConfig();

        assertThat(defaultConfig.getConfig().getString("node.name"), containsString("yggdrash"));

        System.out.println("yggdrash.conf node.name: " + defaultConfig.getConfig().getString("node.name"));

    }

    /**
     * This is the new config file test code.
     * get system config, spring config, yggdrash.conf & yggdrash_sample.conf config.
     */
    @Test
    public void newConfigFileTest() {
        Config config = ConfigFactory.parseResources("yggdrash_sample.conf");

        DefaultConfig defaultConfig = new DefaultConfig(config);

        assertThat(defaultConfig.getConfig().getString("key.path"), containsString("nodePri2.key"));

        System.out.println("newConfigFile key.path: " + defaultConfig.getConfig().getString("key.path"));

    }
}
