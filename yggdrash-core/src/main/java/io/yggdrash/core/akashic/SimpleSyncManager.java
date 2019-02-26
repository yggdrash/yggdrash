package io.yggdrash.core.akashic;

import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.p2p.PeerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class SimpleSyncManager implements SyncManager {
    private static final Logger log = LoggerFactory.getLogger(SimpleSyncManager.class);

    // TODO blockChain lastIndex
    //private Map<BranchId, Integer> height;
    // TODO pool 

    /*
    @Override
    public void syncBlock(PeerHandler peerHandler, BlockChain blockChain, long limitIndex) {
        List<BlockHusk> blockList;
        do {

            long offset = blockChain.getLastIndex() + 1;
            if (limitIndex > 0 && limitIndex <= offset) {
                return;
            }
            blockList = peerHandler.simpleSyncBlock(blockChain.getBranchId(), offset);
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
        List<TransactionHusk> txList = peerHandler.simpleSyncTransaction(blockChain.getBranchId());
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
    */

    @Override
    public void syncBlock(PeerHandler peerHandler, BlockChain blockChain, long limitIndex) {
        long offset = blockChain.getLastIndex() + 1;
        if (limitIndex > 0 && limitIndex <= offset) {
            return;
        }

        BranchId branchId = blockChain.getBranchId();
        Future<List<BlockHusk>> futureHusks = peerHandler.syncBlock(branchId, offset);

        if (futureHusks.isDone()) {
            try {
                List<BlockHusk> blockHusks = futureHusks.get();
                log.debug("[SyncManager] Synchronize block offset={} receivedSize={}, from={}",
                        offset, blockHusks.size(), peerHandler.getPeer());

                for (BlockHusk blockHusk : blockHusks) {
                    if (limitIndex > 0 && limitIndex <= blockHusk.getIndex()) {
                        return;
                    }
                    blockChain.addBlock(blockHusk, false);
                }
            } catch (InterruptedException | ExecutionException e) {
                log.debug("[SyncManager] Sync Block ERR occurred: {}", e.getMessage(), e);
            }
        }
    }

    @Override
    public void syncTransaction(PeerHandler peerHandler, BlockChain blockChain) {
        Future<List<TransactionHusk>> futureHusks = peerHandler.syncTx(blockChain.getBranchId());

        if (futureHusks.isDone()) {
            try {
                List<TransactionHusk> txHusks = futureHusks.get();
                log.debug("[SyncManager] Synchronize Tx receivedSize={}, from={}",
                        txHusks.size(), peerHandler.getPeer());

                for (TransactionHusk txHusk : txHusks) {
                    try {
                        blockChain.addTransaction(txHusk);
                    } catch (Exception e) {
                        log.warn("[SyncManager] Add Tx ERR occurred: {}", e.getMessage());
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                log.debug("[SyncManager] Sync Tx ERR occurred: {}", e.getMessage(), e);
            }
        }
    }
}