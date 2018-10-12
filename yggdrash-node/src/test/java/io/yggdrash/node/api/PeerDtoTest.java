package io.yggdrash.node.api;

import io.yggdrash.core.BranchId;
import io.yggdrash.core.net.Peer;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import static org.assertj.core.api.Assertions.assertThat;

public class PeerDtoTest {

    @Test
    public void valueOf() {
        Peer peer = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");
        PeerDto peerDto = PeerDto.valueOf(BranchId.STEM, peer);
        assertInternal(peerDto, peer);
    }

    @Test
    public void toPeer() {
        PeerDto peerDto = new PeerDto();
        peerDto.setBranchId(BranchId.STEM);
        peerDto.setPeerId("75bff16c");
        peerDto.setIp("localhost");
        peerDto.setPort(32918);
        Peer peer = peerDto.toPeer();
        assertInternal(peerDto, peer);
    }

    private void assertInternal(PeerDto peerDto, Peer peer) {
        assertThat(peerDto.getBranchId()).isEqualTo(BranchId.stem().toString());
        assert Hex.toHexString(peer.getId()).equals(peerDto.getPeerId());
        assert peer.getHost().equals(peerDto.getIp());
        assert peer.getPort() == peerDto.getPort();

    }
}