package io.yggdrash.core.net;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class PeerTest {

    @Test
    public void valueOf() {
        Peer peer = Peer.valueOf("ynode://75bff16c22e6b38c71fd2005657827acce3dfd4a1db1cc417303e42" +
                "9d7da9625525ba3f1b7794e104397467f8c5a11c8e86af4ffcc0aefcdf7024013cdc0508d" +
                "@yggdrash-node1:9090");
        Assertions.assertThat(peer.getHost()).isEqualTo("yggdrash-node1");
        Assertions.assertThat(peer.getPort()).isEqualTo(9090);
    }

    @Test
    public void getYnodeUri() {
        Peer peer = Peer.valueOf("75bff16c", "yggdrash-node1", 9090);
        Assertions.assertThat(peer.getYnodeUri()).isEqualTo("ynode://75bff16c@yggdrash-node1:9090");
    }
}