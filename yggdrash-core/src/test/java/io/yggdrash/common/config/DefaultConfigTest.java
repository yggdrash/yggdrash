package io.yggdrash.common.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultConfigTest {

    private static final Logger log = LoggerFactory.getLogger(DefaultConfigTest.class);

    private DefaultConfig defaultConfig;

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

        assertThat(defaultConfig.getString("java.vm.version")).isNotEmpty();
        log.debug("DefaultConfig java.vm.version: "
                + defaultConfig.getString("java.vm.version"));

        assertThat(defaultConfig.getString(Constants.NODE_GRPC_PORT)).isEqualTo("32918");
        log.debug("DefaultConfig {}: {}",
                Constants.NODE_GRPC_PORT,
                defaultConfig.getString(Constants.NODE_GRPC_PORT));
    }

    /**
     * This is the test for new config file yggdrash_sample.conf.
     * get system config, spring config, yggdrash.conf & yggdrash_sample.conf config.
     */
    @Test
    public void newConfigFileTest() {
        Config config = ConfigFactory.parseResources("yggdrash_sample.conf");
        DefaultConfig defaultConfig = new DefaultConfig(config);

        assertThat(defaultConfig.getString(Constants.YGGDRASH_KEY_PATH)).endsWith("nodePri2.key");

        log.debug("newConfigFile key.path: "
                + defaultConfig.getString(Constants.YGGDRASH_KEY_PATH));
    }

    /**
     * This is the test for printing Class.
     */
    @Test
    public void testNodeInfo() {
        assertThat(defaultConfig.getNetworkP2PVersion()).isEqualTo("0.0.1");
        assertThat(defaultConfig.getNetwork()).isEqualTo(DefaultConfig.Network.TEST_NET);
    }

}
