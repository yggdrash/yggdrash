package io.yggdrash.core.akashic;

import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.core.p2p.PeerHandler;
import io.yggdrash.core.p2p.PeerHandlerMock;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleSyncManagerTest {

    private static final Peer OWNER = Peer.valueOf("ynode://75bff16c@127.0.0.1:32920");
    private final PeerHandler handler = PeerHandlerMock.dummy(OWNER);
    private SimpleSyncManager syncManager;
    private BlockChain blockChain;

    @Before
    public void setUp() {
        syncManager = new SimpleSyncManager();
        blockChain = BlockChainTestUtils.createBlockChain(false);
    }

    @Test
    public void syncBlock() {
        assertThat(blockChain.getLastIndex()).isEqualTo(0);

        syncManager.syncBlock(handler, blockChain, -1);

        assertThat(blockChain.getLastIndex()).isEqualTo(1);
    }

    @Test
    public void syncTransaction() {
        syncManager.syncTransaction(handler, blockChain);
    }
}