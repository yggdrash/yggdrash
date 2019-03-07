package io.yggdrash.node.service;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.yggdrash.common.utils.ByteUtil;
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

import javax.annotation.PreDestroy;
import java.util.List;

@GrpcService
public class BlockChainService extends BlockChainGrpc.BlockChainImplBase {
    private static final Logger log = LoggerFactory.getLogger(BlockChainService.class);
    private static final NetProto.Empty EMPTY = NetProto.Empty.getDefaultInstance();

    private final BlockChainConsumer blockChainConsumer;
    //private final StreamObserver<Proto.Block> blockStreamObserver;
    //private final StreamObserver<Proto.Transaction> transactionStreamObserver;

    @Autowired
    public BlockChainService(BlockChainConsumer blockChainConsumer) {
        this.blockChainConsumer = blockChainConsumer;
        /*
        this.blockStreamObserver =  new StreamObserver<Proto.Block>() {
            @Override
            public void onNext(Proto.Block block) {
                long id = ByteUtil.byteArrayToLong(
                        block.getHeader().getIndex().toByteArray());
                BlockHusk blockHusk = new BlockHusk(block);
                log.debug("[BlockChainService] Received block: id=[{}], hash={}",
                        id, blockHusk.getHash());

                blockChainConsumer.broadcastBlock(blockHusk);
                //responseObserver.onNext(EMPTY);
            }

            @Override
            public void onError(Throwable t) {
                log.debug("[BlockChainService] Encountered error in broadcastBlock: {}",
                        Status.fromThrowable(t));
            }

            @Override
            public void onCompleted() {
                log.debug("[BlockChainService] Complete broadcast block");
            }
        };
        this.transactionStreamObserver =  new StreamObserver<Proto.Transaction>() {
            @Override
            public void onNext(Proto.Transaction tx) {
                TransactionHusk txHusk = new TransactionHusk(tx);
                log.debug("[BlockChainService] Received transaction: hash={}", txHusk.getHash());

                blockChainConsumer.broadcastTx(txHusk);
                //responseObserver.onNext(EMPTY);
            }

            @Override
            public void onError(Throwable t) {
                log.debug("[BlockChainService] Encountered error in broadcastTx: {}",
                        Status.fromThrowable(t));
            }

            @Override
            public void onCompleted() {
                log.debug("[BlockChainService] Complete broadcast tx");
            }
        };
        */
    }

    @PreDestroy
    private void destroy() {
        //blockStreamObserver.onCompleted();
        //transactionStreamObserver.onCompleted();
    }

    /**
     * Sync block response
     *
     * @param syncLimit        the start branch id, block index and limit to sync
     * @param responseObserver the observer response to the block list
     */
    @Override
    public void simpleSyncBlock(NetProto.SyncLimit syncLimit,
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
    public void simpleSyncTransaction(NetProto.SyncLimit syncLimit,
                                StreamObserver<Proto.TransactionList> responseObserver) {
        log.debug("Received syncTransaction request");
        BranchId branchId = BranchId.of(syncLimit.getBranch().toByteArray());
        List<TransactionHusk> txList = blockChainConsumer.syncTx(branchId);
        Proto.TransactionList.Builder builder = Proto.TransactionList.newBuilder();
        for (TransactionHusk husk : txList) {
            builder.addTransactions(husk.getInstance());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }


    @Override
    public void simpleBroadcastBlock(Proto.Block request,
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
    public void simpleBroadcastTransaction(Proto.Transaction request,
                                     StreamObserver<NetProto.Empty> responseObserver) {
        TransactionHusk tx = new TransactionHusk(request);
        log.debug("Received transaction: hash={}", tx.getHash());
        blockChainConsumer.broadcastTx(tx);
        responseObserver.onNext(EMPTY);
        responseObserver.onCompleted();
    }

    /*
    @Override
    public StreamObserver<Proto.Block> broadcastBlock(
            StreamObserver<NetProto.Empty> responseObserver) {
        return blockStreamObserver;
    }

    @Override
    public StreamObserver<Proto.Transaction> broadcastTx(
            StreamObserver<NetProto.Empty> responseObserver) {
        return transactionStreamObserver;
    }
    */

    @Override
    public StreamObserver<Proto.Block> broadcastBlock(
            StreamObserver<NetProto.Empty> responseObserver) {
        return new StreamObserver<Proto.Block>() {
            @Override
            public void onNext(Proto.Block block) {
                long id = ByteUtil.byteArrayToLong(
                        block.getHeader().getIndex().toByteArray());
                BlockHusk blockHusk = new BlockHusk(block);
                log.debug("[BlockChainService] Received block: id=[{}], hash={}",
                        id, blockHusk.getHash());

                blockChainConsumer.broadcastBlock(blockHusk);
                //responseObserver.onNext(EMPTY);
            }

            @Override
            public void onError(Throwable t) {
                log.debug("[BlockChainService] Encountered error in broadcastBlock: {}",
                        Status.fromThrowable(t));
            }

            @Override
            public void onCompleted() {
                log.debug("[BlockChainService] Complete broadcast block");
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
                TransactionHusk txHusk = new TransactionHusk(tx);
                log.debug("[BlockChainService] Received transaction: hash={}", txHusk.getHash());

                blockChainConsumer.broadcastTx(txHusk);
                //responseObserver.onNext(EMPTY);
            }

            @Override
            public void onError(Throwable t) {
                log.debug("[BlockChainService] Encountered error in broadcastTx: {}",
                        Status.fromThrowable(t));
            }

            @Override
            public void onCompleted() {
                log.debug("[BlockChainService] Complete broadcast tx");
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<NetProto.SyncLimit> syncBlock(
            StreamObserver<Proto.BlockList> responseObserver) {
        return new StreamObserver<NetProto.SyncLimit>() {
            @Override
            public void onNext(NetProto.SyncLimit syncLimit) {
                BranchId branchId = BranchId.of(syncLimit.getBranch().toByteArray());
                long offset = syncLimit.getOffset();
                long limit = syncLimit.getLimit();
                log.debug("[BlockChainService] Received syncBlock request:"
                                + "branch={}, offset={}, limit={}, from={}",
                        branchId, offset, limit, syncLimit.getFrom());

                List<BlockHusk> blockList = blockChainConsumer.syncBlock(branchId, offset, limit);
                Proto.BlockList.Builder builder = Proto.BlockList.newBuilder();
                for (BlockHusk block : blockList) {
                    builder.addBlocks(block.getInstance());
                }
                responseObserver.onNext(builder.build());
            }

            @Override
            public void onError(Throwable t) {
                log.debug("[BlockChainService] Encountered error in syncBlock: {}",
                        Status.fromThrowable(t));
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<NetProto.SyncLimit> syncTx(
            StreamObserver<Proto.TransactionList> responseObserver) {
        return new StreamObserver<NetProto.SyncLimit>() {
            @Override
            public void onNext(NetProto.SyncLimit syncLimit) {
                BranchId branchId = BranchId.of(syncLimit.getBranch().toByteArray());
                log.debug("[BlockChainService] Received syncTransaction request");

                List<TransactionHusk> txList = blockChainConsumer.syncTx(branchId);
                Proto.TransactionList.Builder builder = Proto.TransactionList.newBuilder();

                for (TransactionHusk husk : txList) {
                    builder.addTransactions(husk.getInstance());
                }
                responseObserver.onNext(builder.build());
            }

            @Override
            public void onError(Throwable t) {
                log.debug("[BlockChainService] Encountered error in biSyncTx: {}",
                        Status.fromThrowable(t));
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
}
