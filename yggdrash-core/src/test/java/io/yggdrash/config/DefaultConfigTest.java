package io.yggdrash.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class DefaultConfigTest {

    private static final Logger log = LoggerFactory.getLogger(DefaultConfigTest.class);

    DefaultConfig defaultConfig;

    @Before
    public void setUp() {
        this.defaultConfig = new DefaultConfig();
    }

    /**
     * This is the default config test code.
     * get system config, spring config, yggdrash.conf config.
     */
    @Test
    public void defaultConfigTest() {
        log.debug(defaultConfig.toString());

        for (Map.Entry<String, ConfigValue> entry : defaultConfig.getConfig().entrySet()) {
            log.debug("Name:  " + entry.getKey());
            log.debug(entry.toString());
        }

        assertThat(defaultConfig.getConfig().getString("java.version"), containsString("1.8"));
        log.debug("DefaultConfig java.version: "
                + defaultConfig.getConfig().getString("java.version"));

        assertThat(defaultConfig.getConfig().getString("node.name"), containsString("yggdrash"));
        log.debug("DefaultConfig node.name: "
                + defaultConfig.getConfig().getString("node.name"));

        assertThat(defaultConfig.getConfig().getString("network.port"), containsString("32918"));
        log.debug("DefaultConfig network.port: "
                + defaultConfig.getConfig().getString("network.port"));

    }

    /**
     * This is the config test as java version.
     */
    @Test
    public void javaVersionConfigTest() {
        assertThat(defaultConfig.getConfig().getString("java.version"), containsString("1.8"));

        log.debug("DefaultConfig java.version: "
                + defaultConfig.getConfig().getString("java.version"));

    }

    /**
     * This is the config test as yggdrash.conf.
     */
    @Test
    public void yggdrashConfConfigTest() {
        assertThat(defaultConfig.getConfig().getString("node.name"), containsString("yggdrash"));

        log.debug("yggdrash.conf node.name: "
                + defaultConfig.getConfig().getString("node.name"));

    }

    /**
     * This is the test for new config file yggdrash_sample.conf.
     * get system config, spring config, yggdrash.conf & yggdrash_sample.conf config.
     */
    @Test
    public void newConfigFileTest() {
        Config config = ConfigFactory.parseResources("yggdrash_sample.conf");

        DefaultConfig defaultConfig = new DefaultConfig(config);

        assertThat(defaultConfig.getConfig().getString("key.path"), containsString("nodePri2.key"));

        log.debug("newConfigFile key.path: "
                + defaultConfig.getConfig().getString("key.path"));

    }

    /**
     * This is the test for printing Class.
     */
    @Test
    public void testNodeInfo() {
        assert defaultConfig.getNetworkP2PVersion().equals("0.0.1");
        assert defaultConfig.getNetwork() == DefaultConfig.Network.TEST_NET;
        assert defaultConfig.getNodeName().equals("yggdrash");
        assert defaultConfig.getNodeVersion().equals("0.0.2");

    }

}
