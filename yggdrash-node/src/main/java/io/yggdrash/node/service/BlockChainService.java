package io.yggdrash.node.service;

import io.grpc.Status;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@GrpcService
public class BlockChainService extends BlockChainGrpc.BlockChainImplBase {
    private static final Logger log = LoggerFactory.getLogger(BlockChainService.class);
    private static final NetProto.Empty EMPTY = NetProto.Empty.getDefaultInstance();

    private final BlockChainConsumer blockChainConsumer;

    @Autowired
    public BlockChainService(BlockChainConsumer blockChainConsumer) {
        this.blockChainConsumer = blockChainConsumer;
    }

    ///**
    // * Sync block response
    // *
    // * @param syncLimit        the start branch id, block index and limit to sync
    // * @param responseObserver the observer response to the block list
    // */
    /*
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
    */

    ///**
    // * Sync transaction response
    // *
    // * @param syncLimit        the branch id to sync
    // * @param responseObserver the observer response to the transaction list
    // */
    /*
    @Override
    public void simpleSyncTransaction(NetProto.SyncLimit syncLimit,
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
                responseObserver.onNext(EMPTY);
            }

            @Override
            public void onError(Throwable t) {
                log.debug("[BlockChainService] Encountered error in broadcastBlock: {}",
                        Status.fromThrowable(t));
            }

            @Override
            public void onCompleted() {
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
                responseObserver.onNext(EMPTY);
            }

            @Override
            public void onError(Throwable t) {
                log.debug("[BlockChainService] Encountered error in broadcastTx: {}",
                        Status.fromThrowable(t));
            }

            @Override
            public void onCompleted() {
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
                                + "branch={}, offset={}, limit={}", branchId, offset, limit);

                List<BlockHusk> blockList = blockChainConsumer.syncBlock(branchId, offset, limit);
                Proto.BlockList.Builder builder = Proto.BlockList.newBuilder();
                for (BlockHusk block : blockList) {
                    builder.addBlocks(block.getInstance());
                }
                responseObserver.onNext(builder.build());
            }

            @Override
            public void onError(Throwable t) {
                log.debug("[BlockChainService] Encountered error in biSyncBlock: {}",
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

    /*
    [ biDirectTest Overview ]

    Get and return  a StreamObserver response observer, return values via our methods response
    observer while the client is still writing messages to their message stream.
    Each side will always get the other's message in the order they were written,
    bot the client and server can read and write any order - the streams operate completely
    independently.
    */
    private ConcurrentMap<Integer, List<NetProto.Tik>> tikNotes = new ConcurrentHashMap<>();

    /**
     * Receives a stream of msg, and responds with a stream of all previous msgs.
     * @param responseObserver an observer to receive the stream of previous messages.
     * @return an observer to handle requested msg.
     */
    @Override
    public StreamObserver<NetProto.Tik> biDirectTest(
            StreamObserver<NetProto.Tik> responseObserver) {
        return new StreamObserver<NetProto.Tik>() {
            @Override
            public void onNext(NetProto.Tik tik) {
                log.debug("Server :: received tik => " + tik.getMsg());

                NetProto.Tik res = NetProto.Tik.newBuilder()
                        .setSeq(tik.getSeq())
                        .setMsg(tik.getSeq() + " received").build();

                responseObserver.onNext(res);
            }

            @Override
            public void onError(Throwable t) {
                log.debug("BlockChainService :: biDirectTest cancelled " + t.getMessage(), t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
}
