package io.yggdrash.core.net;

import io.yggdrash.PeerTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.common.util.Utils;
import io.yggdrash.core.blockchain.BlockChainManager;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.p2p.Peer;
import io.yggdrash.core.p2p.PeerTableGroup;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class DiscoveryServiceConsumerTest extends TestConstants.SlowTest {
    private final BranchId yggdrash = TestConstants.yggdrash();
    private PeerTableGroup peerTableGroup;
    private DiscoveryConsumer discoveryConsumer;

    @Before
    public void setUp() {
        peerTableGroup = PeerTestUtils.createTableGroup();
        peerTableGroup.createTable(yggdrash);
        discoveryConsumer = new DiscoveryServiceConsumer(peerTableGroup);
    }

    @Test
    public void peerAddedByPingTest() {
        // arrange
        Peer from = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");
        Peer to = Peer.valueOf("ynode://aaaaaaaa@127.0.0.1:32920");

        assertEquals(0, discoveryConsumer.findPeers(yggdrash, from).size());

        // act
        // add peer if address matched
        discoveryConsumer.ping(yggdrash, from, to, "Ping");

        // assert
        assertEquals(1, peerTableGroup.getPeerTable(yggdrash).getBucketsCount());
    }

    @Test
    @Ignore
    public void catchUpRequestByPingTest() {
        // arrange
        discoveryConsumer.setListener(BlockChainSyncManagerMock.mock);

        Peer from = Peer.valueOf("ynode://75bff16c@127.0.0.1:32918");
        from.setBestBlock(1);
        Peer to = Peer.valueOf("ynode://aaaaaaaa@127.0.0.1:32920");

        BlockChainManager blockChainManager
                = BlockChainSyncManagerMock.branchGroup.getBranch(yggdrash).getBlockChainManager();
        long lastIndex = blockChainManager.getLastIndex();

        // catchUpRequest event fired
        discoveryConsumer.ping(yggdrash, from, to, "Ping");

        Utils.sleep(100);
        //assert
        assertNotEquals(lastIndex, blockChainManager.getLastIndex());
    }
}