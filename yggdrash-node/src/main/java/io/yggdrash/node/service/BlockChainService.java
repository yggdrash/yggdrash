package io.yggdrash.node.service;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.core.blockchain.SimpleBlock;
import io.yggdrash.core.blockchain.Transaction;
import io.yggdrash.core.blockchain.TransactionImpl;
import io.yggdrash.core.consensus.ConsensusBlock;
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

    private final BlockChainConsumer<Proto.Block> blockChainConsumer;

    @Autowired
    public BlockChainService(BlockChainConsumer<Proto.Block> blockChainConsumer) {
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
        List<ConsensusBlock<Proto.Block>> blockList = blockChainConsumer.syncBlock(branchId, offset, limit);
        Proto.BlockList.Builder builder = Proto.BlockList.newBuilder();
        for (ConsensusBlock<Proto.Block> block : blockList) {
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
    public void syncTx(NetProto.SyncLimit syncLimit,
                                StreamObserver<Proto.TransactionList> responseObserver) {
        log.debug("Received syncTransaction request");
        BranchId branchId = BranchId.of(syncLimit.getBranch().toByteArray());
        List<Transaction> txList = blockChainConsumer.syncTx(branchId);
        Proto.TransactionList.Builder builder = Proto.TransactionList.newBuilder();
        for (Transaction tx : txList) {
            builder.addTransactions(tx.getInstance());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<Proto.Block> broadcastBlock(
            StreamObserver<NetProto.Empty> responseObserver) {
        return new StreamObserver<Proto.Block>() {
            @Override
            public void onNext(Proto.Block block) {
                long id = block.getHeader().getIndex();
                SimpleBlock simpleBlock = new SimpleBlock(block.toByteArray());
                log.debug("[BlockChainService] Received block: id=[{}], hash={}",
                        id, simpleBlock.getHash());

                blockChainConsumer.broadcastBlock(simpleBlock);
            }

            @Override
            public void onError(Throwable t) {
                log.warn("[BlockChainService] Encountered error in broadcastBlock: {}",
                        Status.fromThrowable(t));
            }

            @Override
            public void onCompleted() {
                log.debug("[BlockChainService] Complete broadcast block");
                responseObserver.onNext(EMPTY);
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<Proto.Transaction> broadcastTx(
            StreamObserver<NetProto.Empty> responseObserver) {
        return new StreamObserver<Proto.Transaction>() {
            @Override
            public void onNext(Proto.Transaction tx) {
                Transaction txHusk = new TransactionImpl(tx);
                log.debug("Received transaction: hash={}, {}", txHusk.getHash(), this);

                blockChainConsumer.broadcastTx(txHusk);
            }

            @Override
            public void onError(Throwable t) {
                log.warn("Encountered error in broadcastTx: {}", Status.fromThrowable(t));
            }

            @Override
            public void onCompleted() {
                log.debug("[BlockChainService] Complete broadcast tx");
                responseObserver.onNext(EMPTY);
                responseObserver.onCompleted();
            }
        };
    }
}
