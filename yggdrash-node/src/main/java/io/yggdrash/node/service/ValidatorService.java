package io.yggdrash.node.service;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.BlockChainBuilder;
import io.yggdrash.core.blockchain.BlockChainImpl;
import io.yggdrash.core.consensus.Consensus;
import io.yggdrash.core.consensus.ConsensusBlockChain;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.store.StoreBuilder;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.EbftProto;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.validator.data.ebft.EbftBlock;
import io.yggdrash.validator.data.pbft.PbftBlock;
import io.yggdrash.validator.data.pbft.PbftMessageSet;
import io.yggdrash.validator.service.ebft.EbftServerStub;
import io.yggdrash.validator.service.ebft.EbftService;
import io.yggdrash.validator.service.node.NodeServerStub;
import io.yggdrash.validator.service.pbft.PbftServerStub;
import io.yggdrash.validator.service.pbft.PbftService;
import io.yggdrash.validator.store.ebft.EbftBlockStore;
import io.yggdrash.validator.store.pbft.PbftBlockStore;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.io.IOException;

public class ValidatorService {

    private final String host;
    private final int port;
    private final Wallet wallet;

    private ThreadPoolTaskScheduler taskScheduler;

    private final Server grpcServer;

    public ValidatorService(DefaultConfig defaultConfig, ConsensusBlockChain blockChain)
            throws IOException, InvalidCipherTextException {
        this.host = defaultConfig.getString("yggdrash.validator.host");
        this.port = defaultConfig.getInt("yggdrash.validator.port");
        this.wallet = new Wallet(defaultConfig.getString("yggdrash.validator.key.path"),
                defaultConfig.getString("yggdrash.validator.key.password"));
        this.taskScheduler = threadPoolTaskScheduler();

        Consensus consensus = blockChain.getConsensus();
        switch (consensus.getAlgorithm()) {
            case "pbft":
                PbftService pbftService = new PbftService(wallet, blockChain, defaultConfig, host, port);
                taskScheduler.schedule(pbftService, new CronTrigger(consensus.getPeriod()));
                try {
                    this.grpcServer = ServerBuilder.forPort(port)
                            .addService(new PbftServerStub(pbftService))
                            .addService(new NodeServerStub(blockChain))
                            .build()
                            .start();
                } catch (IOException e) {
                    throw new NotValidateException("Grpc IOException");
                }
                break;
            case "ebft":
                EbftService ebftService = new EbftService(wallet, blockChain, defaultConfig, host, port);
                taskScheduler.schedule(ebftService, new CronTrigger(consensus.getPeriod()));
                try {
                    this.grpcServer = ServerBuilder.forPort(port)
                            .addService(new EbftServerStub(ebftService))
                            .addService(new NodeServerStub(blockChain))
                            .build()
                            .start();
                } catch (IOException e) {
                    throw new NotValidateException("Grpc IOException");
                }
                break;
            default:
                throw new NotValidateException("Algorithm is not valid.");
        }
    }

    private ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(5);
        threadPoolTaskScheduler.setThreadNamePrefix(host + "_" + port + "_");
        threadPoolTaskScheduler.initialize();
        return threadPoolTaskScheduler;
    }

    public static BlockChainBuilder.Factory factory() {
        return (branch, genesisBlock, blockStore, transactionStore, branchStore, stateStore, transactionReceiptStore,
                contractContainer, outputStores) -> {

            Consensus consensus = new Consensus(branch.getConsensus());

            switch (consensus.getAlgorithm()) {
                case "pbft":
                    PbftBlock pbftBlock = new PbftBlock(genesisBlock, PbftMessageSet.forGenesis());
                    return new BlockChainImpl<PbftProto.PbftBlock, PbftMessageSet>(branch, pbftBlock,
                            blockStore, transactionStore, branchStore, stateStore, transactionReceiptStore,
                            contractContainer, outputStores);
                case "ebft":
                    EbftBlock ebftBlock = new EbftBlock(genesisBlock);
                    return new BlockChainImpl<EbftProto.EbftBlock, PbftMessageSet>(branch, ebftBlock,
                            blockStore, transactionStore, branchStore, stateStore, transactionReceiptStore,
                            contractContainer, outputStores);
                default:
            }
            throw new NotValidateException("Algorithm is not valid.");
        };
    }

    public static StoreBuilder.BlockStoreFactory blockStoreFactory() {
        return (consensusAlgorithm, dbSource) -> {

            switch (consensusAlgorithm) {
                case "pbft":
                    return new PbftBlockStore(dbSource);
                case "ebft":
                    return new EbftBlockStore(dbSource);
                default:
            }
            throw new NotValidateException("Algorithm is not valid.");
        };
    }

    public void shutdown() {
        grpcServer.shutdown();
    }
}
