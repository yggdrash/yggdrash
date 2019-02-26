package io.yggdrash.core.akashic;

import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.p2p.PeerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class SimpleSyncManager implements SyncManager {
    private static final Logger log = LoggerFactory.getLogger(SimpleSyncManager.class);

    // TODO blockChain lastIndex
    //private Map<BranchId, Integer> height;
    // TODO pool 

    @Override
    public void syncBlock(PeerHandler peerHandler, BlockChain blockChain, long limitIndex) {
        List<BlockHusk> blockList;
        do {
            long offset = blockChain.getLastIndex() + 1;
            if (limitIndex > 0 && limitIndex <= offset) {
                return;
            }
            blockList = peerHandler.syncBlock(blockChain.getBranchId(), offset);
            log.debug("Synchronize block offset={} receivedSize={}, from={}", offset, blockList.size(),
                    peerHandler.getPeer());
            for (BlockHusk block : blockList) {
                if (limitIndex > 0 && limitIndex <= block.getIndex()) {
                    return;
                }
                blockChain.addBlock(block, false);
            }
        } while (!blockList.isEmpty());
    }

    @Override
    public void syncTransaction(PeerHandler peerHandler, BlockChain blockChain) {
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