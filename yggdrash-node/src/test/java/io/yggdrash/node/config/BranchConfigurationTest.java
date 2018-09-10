package io.yggdrash.node.config;

import io.yggdrash.core.BranchGroup;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.env.MockEnvironment;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;

public class BranchConfigurationTest {
    static final ResourceLoader loader = new DefaultResourceLoader();
    MockEnvironment mockEnv;
    BranchProperties properties;

    @Before
    public void setUp() {
        mockEnv = new MockEnvironment();
        properties = new BranchProperties();
    }

    @Test
    public void defaultBranchGroupTest() throws IOException {
        assert getBranchGroup().getBranchSize() == 1;
    }

    @Test
    public void productionBranchGroupTest() throws IOException {
        mockEnv.addActiveProfile("prod");
        assert getBranchGroup().getBranchSize() == 1;
    }

    @Test(expected = FileNotFoundException.class)
    public void unknownBranchGroupTest() throws IOException {
        properties.setNameList(Collections.singletonList("dart"));
        getBranchGroup();
    }

    private BranchGroup getBranchGroup() throws IOException {
        BranchConfiguration config = new BranchConfiguration(properties, loader, mockEnv);
        return config.branchGroup();
    }
}