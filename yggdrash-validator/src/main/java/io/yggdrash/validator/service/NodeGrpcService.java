package io.yggdrash.validator.service;

import io.grpc.stub.StreamObserver;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.proto.BlockChainGrpc;
import io.yggdrash.proto.NetProto;
import io.yggdrash.proto.Proto;
import io.yggdrash.validator.data.BlockCon;
import io.yggdrash.validator.data.BlockConChain;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@GRpcService
@ConditionalOnProperty(name = "yggdrash.validator.consensus.algorithm", havingValue = "ebft")
public class NodeGrpcService extends BlockChainGrpc.BlockChainImplBase {
    private static final Logger log = LoggerFactory.getLogger(NodeGrpcService.class);
    private static final NetProto.Empty EMPTY = NetProto.Empty.getDefaultInstance();

    private final BlockConChain blockConChain;

    @Autowired
    public NodeGrpcService(BlockConChain blockConChain) {
        this.blockConChain = blockConChain;
    }

    @Override
    public void syncBlock(NetProto.SyncLimit syncLimit,
                          StreamObserver<Proto.BlockList> responseObserver) {
        log.debug("NodeService syncBlock");
        long offset = syncLimit.getOffset();
        long limit = syncLimit.getLimit();
        log.debug("syncBlock() request offset={}, limit={}", offset, limit);

        Proto.BlockList.Builder builder = Proto.BlockList.newBuilder();
        if (Arrays.equals(syncLimit.getBranch().toByteArray(), blockConChain.getChain())
                && offset >= 0
                && offset <= blockConChain.getLastConfirmedBlockCon().getIndex()) {
            List<BlockCon> blockConList = blockConChain.getBlockConList(offset, limit);
            for (BlockCon blockCon : blockConList) {
                builder.addBlocks(blockCon.getBlock().toProtoBlock());
            }
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void syncTransaction(NetProto.SyncLimit syncLimit,
                                StreamObserver<Proto.TransactionList> responseObserver) {
        log.debug("NodeService syncTransaction");
        long offset = syncLimit.getOffset();
        long limit = syncLimit.getLimit();
        log.debug("syncTransaction() request offset={}, limit={}", offset, limit);

        Proto.TransactionList.Builder builder = Proto.TransactionList.newBuilder();
        if (Arrays.equals(syncLimit.getBranch().toByteArray(), blockConChain.getChain())) {
            for (TransactionHusk husk :
                    new ArrayList<>(blockConChain.getTransactionStore().getUnconfirmedTxs())) {
                builder.addTransactions(husk.getInstance());
            }
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void broadcastBlock(Proto.Block request,
                               StreamObserver<NetProto.Empty> responseObserver) {
        // Validator donot need to receive blocks from general node
        log.debug("NodeService broadcastBlock");
        responseObserver.onNext(EMPTY);
        responseObserver.onCompleted();
    }

    @Override
    public void broadcastTransaction(Proto.Transaction request,
                                     StreamObserver<NetProto.Empty> responseObserver) {
        log.debug("NodeService broadcastTransaction");
        log.debug("Received transaction: {}", request);
        TransactionHusk tx = new TransactionHusk(request);
        if (Arrays.equals(tx.getBranchId().getBytes(), blockConChain.getChain())) {
            blockConChain.getTransactionStore().put(tx.getHash(), tx);
        }

        responseObserver.onNext(EMPTY);
        responseObserver.onCompleted();
    }
}
