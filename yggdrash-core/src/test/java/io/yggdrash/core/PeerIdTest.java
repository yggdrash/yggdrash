package io.yggdrash.core;

import io.yggdrash.core.net.PeerId;
import org.junit.Test;

public class PeerIdTest {

    private static final String ynodeUri1
            = "ynode://9ea9225f0b7db3c697c0a2e09cdd65046899058d16f73378c1559d61aa3e10cd5dc933714"
            + "2728f5a02faadafab2b926e2998d5bc2b62c2183fab75ca996de2ce@localhost:32918";
    private static final String ynodeUri2
            = "ynode://9ea9225f0b7db3c697c0a2e09cdd65046899058d16f73378c1559d61aa3e10cd5dc933714"
            + "2728f5a02faadafab2b926e2998d5bc2b62c2183fab75ca996de2ce@localhost:32919";

    @Test
    public void of() {
        PeerId peerId1 = PeerId.of(ynodeUri1);
        byte[] b = peerId1.getBytes();
        PeerId peerId2 = PeerId.of(b);
        assert peerId1.equals(peerId2);
    }

    @Test
    public void shouldBeSameDistance() {
        PeerId peerId1 = PeerId.of(ynodeUri1);
        PeerId peerId2 = PeerId.of(ynodeUri2);
        assert peerId1.distanceTo(peerId2.getBytes()) == peerId2.distanceTo(peerId1.getBytes());
    }
}