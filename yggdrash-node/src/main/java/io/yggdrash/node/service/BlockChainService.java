package io.yggdrash.node.service;

import io.grpc.stub.StreamObserver;
import io.yggdrash.common.util.ByteUtil;
import io.yggdrash.core.blockchain.BlockChain;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.proto.BlockChainGrpc;
import io.yggdrash.proto.NetProto;
import io.yggdrash.proto.Proto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockChainService extends BlockChainGrpc.BlockChainImplBase {
    private static final Logger log = LoggerFactory.getLogger(BlockChainService.class);
    private static final NetProto.Empty EMPTY = NetProto.Empty.getDefaultInstance();

    private final BranchGroup branchGroup;

    public BlockChainService(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }

    /**
     * Sync block response
     *
     * @param syncLimit        the start branch id, block index and limit to sync
     * @param responseObserver the observer response to the block list
     */
    @Override
    public void syncBlock(NetProto.SyncLimit syncLimit,
                          StreamObserver<Proto.BlockList> responseObserver) {
        long offset = syncLimit.getOffset();
        BranchId branchId = BranchId.of(syncLimit.getBranch().toByteArray());
        BlockChain blockChain = branchGroup.getBranch(branchId);
        Proto.BlockList.Builder builder = Proto.BlockList.newBuilder();
        if (blockChain == null) {
            log.warn("Invalid syncBlock request for branchId={}", branchId);
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
            return;
        }
        if (offset < 0) {
            offset = 0;
        }
        long limit = syncLimit.getLimit();
        log.debug("Received syncBlock request offset={}, limit={}", offset, limit);

        for (int i = 0; i < limit; i++) {
            BlockHusk block = branchGroup.getBlockByIndex(branchId, offset++);
            if (block == null) {
                break;
            }
            builder.addBlocks(block.getInstance());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    /**
     * Sync transaction response
     *
     * @param syncLimit        the branch id to sync
     * @param responseObserver the observer response to the transaction list
     */
    @Override
    public void syncTransaction(NetProto.SyncLimit syncLimit,
                                StreamObserver<Proto.TransactionList> responseObserver) {
        log.debug("Received syncTransaction request");
        BranchId branchId = BranchId.of(syncLimit.getBranch().toByteArray());
        Proto.TransactionList.Builder builder = Proto.TransactionList.newBuilder();
        for (TransactionHusk husk : branchGroup.getUnconfirmedTxs(branchId)) {
            builder.addTransactions(husk.getInstance());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void broadcastBlock(Proto.Block request,
                               StreamObserver<NetProto.Empty> responseObserver) {
        long id = ByteUtil.byteArrayToLong(
                request.getHeader().getIndex().toByteArray());
        BlockHusk block = new BlockHusk(request);
        log.debug("Received block id=[{}], hash={}", id, block.getHash());
        try {
            branchGroup.addBlock(block, true);
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
        responseObserver.onNext(EMPTY);
        responseObserver.onCompleted();
    }

    @Override
    public void broadcastTransaction(Proto.Transaction request,
                                     StreamObserver<NetProto.Empty> responseObserver) {
        log.debug("Received transaction: {}", request);
        TransactionHusk tx = new TransactionHusk(request);
        try {
            branchGroup.addTransaction(tx);
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
        responseObserver.onNext(EMPTY);
        responseObserver.onCompleted();
    }
}
