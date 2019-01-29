package io.yggdrash.core.net;

import io.yggdrash.PeerTestUtils;
import org.junit.Test;

public class BootStrapNodeTest {

    @Test
    public void selfLookupTest() {
        YggdrashTestNode node1 = new YggdrashTestNode();
        node1.bootstrapping();
    }

    private class YggdrashTestNode extends BootStrapNode {
        YggdrashTestNode() {
            setDht(PeerTestUtils.createTable());
        }
    }
}