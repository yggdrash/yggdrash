package io.yggdrash.node.config;

import io.yggdrash.core.BranchGroup;
import io.yggdrash.core.Wallet;
import io.yggdrash.core.net.PeerGroup;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;

public class BranchConfigurationTest {

    MockEnvironment mockEnv;
    PeerGroup peerGroup = new PeerGroup(1);
    ResourceLoader loader = new DefaultResourceLoader();

    @Before
    public void setUp() {
        mockEnv = new MockEnvironment();
    }

    @Test
    public void defaultBranchGroupTest() throws IOException, InvalidCipherTextException {
        assert getBranchGroup().getBranchSize() == 1;
    }

    @Test
    public void productionBranchGroupTest() throws IOException, InvalidCipherTextException {
        mockEnv.addActiveProfile("prod");
        assert getBranchGroup().getBranchSize() == 1;
    }

    private BranchGroup getBranchGroup() throws IOException, InvalidCipherTextException {
        BranchConfiguration config = new BranchConfiguration(mockEnv, new Wallet(), peerGroup);
        config.setResource(loader.getResource("classpath:/genesis.json"));
        return config.branchGroup();
    }
}