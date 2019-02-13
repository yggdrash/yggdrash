package io.yggdrash.core.akashic;

import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.net.NodeStatus;
import io.yggdrash.core.net.PeerHandler;
import io.yggdrash.core.net.PeerHandlerGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SimpleSyncManager implements SyncManager {
    private static final Logger log = LoggerFactory.getLogger(SimpleSyncManager.class);

    private final NodeStatus nodeStatus;
    private final BranchGroup branchGroup;
    private final PeerHandlerGroup peerHandlerGroup;

    public SimpleSyncManager(BranchGroup branchGroup,
                             PeerHandlerGroup peerHandlerGroup,
                             NodeStatus nodeStatus) {
        this.branchGroup = branchGroup;
        this.peerHandlerGroup = peerHandlerGroup;
        this.nodeStatus = nodeStatus;
    }

    @Override
    public void syncBlockAndTransaction() {
        try {
            nodeStatus.sync();
            for (BlockChain blockChain : branchGroup.getAllBranch()) {
                List<PeerHandler> peerHandlerList =
                        peerHandlerGroup.getHandlerList(blockChain.getBranchId());
                for (PeerHandler peerHandler : peerHandlerList) {
                    syncTransaction(peerHandler, blockChain);
                    syncBlock(peerHandler, blockChain);
                }
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        } finally {
            nodeStatus.up();
        }
    }

    private void syncBlock(PeerHandler peerHandler, BlockChain blockChain) {
        List<BlockHusk> blockList;
        do {
            long offset = blockChain.getLastIndex() + 1;
            blockList = peerHandler.syncBlock(blockChain.getBranchId(), offset);
            log.debug("Synchronize block offset={} receivedSize={}, from={}", offset, blockList.size(),
                    peerHandler.getPeer());
            for (BlockHusk block : blockList) {
                blockChain.addBlock(block, false);
            }
        } while (!blockList.isEmpty());
    }

    private void syncTransaction(PeerHandler peerHandler, BlockChain blockChain) {
        List<TransactionHusk> txList = peerHandler.syncTransaction(blockChain.getBranchId());
        log.info("Synchronize transaction receivedSize={}, from={}", txList.size(),
                peerHandler.getPeer());
        for (TransactionHusk tx : txList) {
            try {
                blockChain.addTransaction(tx);
            } catch (Exception e) {
                log.warn(e.getMessage());
            }
        }
    }

}
