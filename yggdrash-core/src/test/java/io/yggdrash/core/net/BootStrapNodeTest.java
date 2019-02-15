package io.yggdrash.core.net;

import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.p2p.PeerHandler;
import org.junit.Test;

import java.util.List;

public class BootStrapNodeTest {

    @Test
    public void selfLookupTest() {
        YggdrashTestNode node1 = new YggdrashTestNode();
        node1.bootstrapping();
    }

    private class YggdrashTestNode extends BootStrapNode {
        YggdrashTestNode() {
            setBranchGroup(new BranchGroup());
            setNodeStatus(NodeStatusMock.mock);
            setPeerNetwork(new PeerNetwork() {
                @Override
                public void init() {

                }

                @Override
                public void addNetwork(BranchId branchId) {

                }

                @Override
                public List<PeerHandler> getHandlerList(BranchId branchId) {
                    return null;
                }

                @Override
                public void chainedBlock(BlockHusk block) {

                }

                @Override
                public void receivedTransaction(TransactionHusk tx) {

                }
            });
        }
    }
}