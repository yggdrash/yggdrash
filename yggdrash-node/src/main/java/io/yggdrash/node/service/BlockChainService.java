package io.yggdrash.node.service;

import io.grpc.stub.StreamObserver;
import io.yggdrash.common.util.ByteUtil;
import io.yggdrash.core.blockchain.BlockHusk;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.core.net.BlockChainConsumer;
import io.yggdrash.node.springboot.grpc.GrpcService;
import io.yggdrash.proto.BlockChainGrpc;
import io.yggdrash.proto.NetProto;
import io.yggdrash.proto.Proto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@GrpcService
public class BlockChainService extends BlockChainGrpc.BlockChainImplBase {
    private static final Logger log = LoggerFactory.getLogger(BlockChainService.class);
    private static final NetProto.Empty EMPTY = NetProto.Empty.getDefaultInstance();

    private BlockChainConsumer blockChainConsumer;

    @Autowired
    BlockChainService(BlockChainConsumer blockChainConsumer) {
        this.blockChainConsumer = blockChainConsumer;
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
        BranchId branchId = BranchId.of(syncLimit.getBranch().toByteArray());
        long offset = syncLimit.getOffset();
        long limit = syncLimit.getLimit();
        log.debug("Received syncBlock request branch={} offset={}, limit={}",
                branchId, offset, limit);
        List<BlockHusk> blockList = blockChainConsumer.syncBlock(branchId, offset, limit);
        Proto.BlockList.Builder builder = Proto.BlockList.newBuilder();
        for (BlockHusk block : blockList) {
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
        List<TransactionHusk> txList = blockChainConsumer.syncTransaction(branchId);
        Proto.TransactionList.Builder builder = Proto.TransactionList.newBuilder();
        for (TransactionHusk husk : txList) {
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
        blockChainConsumer.broadcastBlock(block);
        responseObserver.onNext(EMPTY);
        responseObserver.onCompleted();
    }

    @Override
    public void broadcastTransaction(Proto.Transaction request,
                                     StreamObserver<NetProto.Empty> responseObserver) {
        TransactionHusk tx = new TransactionHusk(request);
        log.debug("Received transaction: hash={}", tx.getHash());
        blockChainConsumer.broadcastTransaction(tx);
        responseObserver.onNext(EMPTY);
        responseObserver.onCompleted();
    }
}
