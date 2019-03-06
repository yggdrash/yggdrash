package io.yggdrash.core.akashic;

import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.p2p.PeerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SimpleSyncManager implements SyncManager {
    private static final Logger log = LoggerFactory.getLogger(SimpleSyncManager.class);

    @Override
    public void syncBlock(PeerHandler peerHandler, BlockChain blockChain, long limitIndex) {
        // try 10 times per 10,000 blocks = maximum 100,000 blocks per peer
        for (int i = 0; i < 10; i++) {
            long offset = blockChain.getLastIndex() + 1;
            if (limitIndex > 0 && limitIndex <= offset) {
                return;
            }
            List<BlockHusk> blockList = peerHandler.syncBlock(blockChain.getBranchId(), offset);
            log.debug("Synchronize block requestOffset={} receivedSize={}, from={}", offset, blockList.size(),
                    peerHandler.getPeer().toAddress());
            if (blockList.isEmpty()) {
                return;
            }
            for (BlockHusk block : blockList) {
                if (limitIndex > 0 && limitIndex <= block.getIndex()) {
                    return;
                }
                blockChain.addBlock(block, false);
            }
        }
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
