package io.yggdrash.core.p2p;

import io.yggdrash.core.exception.NotValidateException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PeerTest {

    @Test
    public void createPeerTest() {
        Peer peer = Peer.valueOf("ynode://75bff16c22e6b38c71fd2005657827acce3dfd4a1db1cc417303e42"
                + "9d7da9625525ba3f1b7794e104397467f8c5a11c8e86af4ffcc0aefcdf7024013cdc0508d"
                + "@yggdrash-node1:32918");
        assertThat(peer.getHost()).isEqualTo("yggdrash-node1");
        assertThat(peer.getPort()).isEqualTo(32918);
    }

    @Test
    public void createPeerWithNodeIdTest() {
        Peer peer1 = Peer.valueOf("75bff16c", "yggdrash-node1", 32918);
        assertThat(peer1.getYnodeUri()).isEqualTo("ynode://75bff16c@yggdrash-node1:32918");
    }

    @Test(expected = NotValidateException.class)
    public void unknownSchemaTest() {
        Peer.valueOf("http://75bff16c@yggdrash-node1:32918");
    }

    @Test
    public void equalsTest() {
        Peer peer1 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");
        assertThat(peer1).isEqualTo(Peer.valueOf("ynode://75bff16c@127.0.0.1:32918"));

        Peer peer2 = Peer.valueOf("ynode://75bff16c@127.0.0.1:32919");
        assertThat(peer1).isNotEqualTo(peer2);
    }

    @Test
    public void isLocalTest() {
        assertThat(Peer.valueOf("ynode://75bff16c@127.0.0.1:32918").isLocal()).isTrue();
        assertThat(Peer.valueOf("ynode://75bff16c@localhost:32918").isLocal()).isTrue();
        assertThat(Peer.valueOf("ynode://75bff16c@yggdrash-node1:32918").isLocal()).isFalse();
    }
}