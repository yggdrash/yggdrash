package io.yggdrash.validator.service.node;

import io.grpc.stub.StreamObserver;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.blockchain.TransactionHusk;
import io.yggdrash.proto.BlockChainGrpc;
import io.yggdrash.proto.NetProto;
import io.yggdrash.proto.Proto;
import io.yggdrash.validator.data.ConsensusBlock;
import io.yggdrash.validator.data.ConsensusBlockChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NodeServerStub extends BlockChainGrpc.BlockChainImplBase {
    private static final Logger log = LoggerFactory.getLogger(NodeServerStub.class);
    private static final NetProto.Empty EMPTY = NetProto.Empty.getDefaultInstance();

    private final ConsensusBlockChain blockChain;

    public NodeServerStub(ConsensusBlockChain blockChain) {
        this.blockChain = blockChain;
    }

    @Override
    public void syncBlock(NetProto.SyncLimit syncLimit,
                                StreamObserver<Proto.BlockList> responseObserver) {
        log.debug("NodeService syncBlock");
        long offset = syncLimit.getOffset();
        long limit = syncLimit.getLimit();
        log.debug("syncBlock() request offset={}, limit={}", offset, limit);

        Proto.BlockList.Builder builder = Proto.BlockList.newBuilder();
        if (Arrays.equals(syncLimit.getBranch().toByteArray(), blockChain.getChain())
                && offset >= 0
                && offset <= blockChain.getLastConfirmedBlock().getIndex()) {
            List blockList = blockChain.getBlockList(offset, limit);
            for (Object object : blockList) {
                ConsensusBlock consensusBlock = (ConsensusBlock) object;
                if (consensusBlock.getBlock() != null) {
                    builder.addBlocks(Block.toProtoBlock(consensusBlock.getBlock()));
                }
            }
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void syncTx(NetProto.SyncLimit syncLimit,
                                      StreamObserver<Proto.TransactionList> responseObserver) {
        log.debug("NodeService syncTransaction");
        long offset = syncLimit.getOffset();
        long limit = syncLimit.getLimit();
        log.debug("syncTransaction() request offset={}, limit={}", offset, limit);

        Proto.TransactionList.Builder builder = Proto.TransactionList.newBuilder();
        if (Arrays.equals(syncLimit.getBranch().toByteArray(), blockChain.getChain())) {
            //todo: check memory leak
            for (TransactionHusk husk :
                    new ArrayList<>(blockChain.getTransactionStore().getUnconfirmedTxs())) {
                builder.addTransactions(husk.getInstance());
            }
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<Proto.Block> broadcastBlock(StreamObserver<NetProto.Empty> responseObserver) {
        return new StreamObserver<Proto.Block>() {
            @Override
            public void onNext(Proto.Block value) {
                log.warn("ignored broadcast block");
            }

            @Override
            public void onError(Throwable t) {
                log.warn(t.getMessage());
            }

            @Override
            public void onCompleted() {
                // Validator do not need to receive blocks from general node
                log.debug("NodeService broadcastBlock");
                responseObserver.onNext(EMPTY);
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<Proto.Transaction> broadcastTx(StreamObserver<NetProto.Empty> responseObserver) {
        return new StreamObserver<Proto.Transaction>() {
            @Override
            public void onNext(Proto.Transaction value) {
                log.debug("NodeService broadcastTransaction");
                log.debug("Received transaction: {}", value);
                TransactionHusk tx = new TransactionHusk(value);
                if (Arrays.equals(tx.getBranchId().getBytes(), blockChain.getChain())) {
                    blockChain.getTransactionStore().put(tx.getHash(), tx);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.warn(t.getMessage());
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(EMPTY);
                responseObserver.onCompleted();
            }
        };
    }
}
