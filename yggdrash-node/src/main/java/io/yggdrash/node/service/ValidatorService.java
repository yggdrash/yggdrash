package io.yggdrash.node.service;

import ch.qos.logback.classic.Level;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.BlockChainBuilder;
import io.yggdrash.core.blockchain.BlockChainImpl;
import io.yggdrash.core.consensus.Consensus;
import io.yggdrash.core.consensus.ConsensusBlockChain;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.store.BlockStoreFactory;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.proto.EbftProto;
import io.yggdrash.proto.PbftProto;
import io.yggdrash.validator.data.ebft.EbftBlock;
import io.yggdrash.validator.data.pbft.PbftBlock;
import io.yggdrash.validator.data.pbft.PbftMessageSet;
import io.yggdrash.validator.service.ebft.EbftServerStub;
import io.yggdrash.validator.service.ebft.EbftService;
import io.yggdrash.validator.service.node.DiscoveryServiceStub;
import io.yggdrash.validator.service.pbft.PbftServerStub;
import io.yggdrash.validator.service.pbft.PbftService;
import io.yggdrash.validator.store.ebft.EbftBlockStore;
import io.yggdrash.validator.store.pbft.PbftBlockStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import java.io.IOException;

public class ValidatorService {
    private static final String NOT_VALID_MSG = "Algorithm is not valid.";
    private final String host;
    private final int port;
    private final Wallet wallet;
    private static final Logger log = LoggerFactory.getLogger(ValidatorService.class);

    private ThreadPoolTaskScheduler taskScheduler;

    private final Server grpcServer;

    public ValidatorService(DefaultConfig defaultConfig, ConsensusBlockChain blockChain, TransactionService txService)
            throws IOException, InvalidCipherTextException {
        this.host = defaultConfig.getString(Constants.VALIDATOR_GRPC_HOST_CONF);
        this.port = defaultConfig.getInt(Constants.VALIDATOR_GRPC_PORT_CONF);
        this.wallet = new Wallet(defaultConfig.getString(Constants.YGGDRASH_KEY_PATH),
                defaultConfig.getString(Constants.YGGDRASH_KEY_PASSWORD));

        log.info("ValidatorService init");

        //setLogLevel(defaultConfig);

        this.taskScheduler = threadPoolTaskScheduler();
        // TODO to factory Patten
        Consensus consensus = blockChain.getConsensus();

        switch (consensus.getAlgorithm()) {
            case "pbft":
                PbftService pbftService = new PbftService(wallet, blockChain, defaultConfig, host, port);
                taskScheduler.schedule(pbftService, new CronTrigger(consensus.getPeriod()));
                try {
                    this.grpcServer = ServerBuilder.forPort(port)
                            .addService(new PbftServerStub(pbftService))
                            .addService(new DiscoveryServiceStub(blockChain, pbftService))
                            .addService(txService)
                            .build()
                            .start();

                } catch (IOException e) {
                    throw new NotValidateException("Grpc IOException");
                }
                break;
            case "ebft":
                // TODO remove EBFT
                EbftService ebftService = new EbftService(wallet, blockChain, defaultConfig, host, port);
                taskScheduler.schedule(ebftService, new CronTrigger(consensus.getPeriod()));
                try {
                    this.grpcServer = ServerBuilder.forPort(port)
                            .addService(new EbftServerStub(ebftService))
                            .addService(txService)
                            .build()
                            .start();
                } catch (IOException e) {
                    throw new NotValidateException("Grpc IOException");
                }
                break;
            default:
                throw new NotValidateException(NOT_VALID_MSG);
        }
    }

    private void setLogLevel(DefaultConfig defaultConfig) {
        String logLevel = defaultConfig.getString(Constants.VALIDATOR_LOG_LEVEL_CONF);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.yggdrash.validator"))
                .setLevel(Level.toLevel(logLevel, Level.INFO));
    }

    private ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(5);
        threadPoolTaskScheduler.setThreadNamePrefix(host + "_" + port + "_");
        threadPoolTaskScheduler.initialize();
        return threadPoolTaskScheduler;
    }

    public static BlockChainBuilder.Factory factory() {
        return (branch, genesisBlock, branchStore, blockChainManager, contractManager) -> {

            Consensus consensus = new Consensus(branch.getConsensus());

            switch (consensus.getAlgorithm()) {
                case "pbft":
                    PbftBlock pbftBlock = new PbftBlock(genesisBlock, PbftMessageSet.forGenesis());
                    return new BlockChainImpl<PbftProto.PbftBlock, PbftMessageSet>(
                            branch, pbftBlock, branchStore, blockChainManager, contractManager);
                case "ebft":
                    EbftBlock ebftBlock = new EbftBlock(genesisBlock);
                    return new BlockChainImpl<EbftProto.EbftBlock, EbftBlock>(
                            branch, ebftBlock, branchStore, blockChainManager, contractManager);
                default:
            }
            throw new NotValidateException(NOT_VALID_MSG);
        };
    }

    public static BlockStoreFactory blockStoreFactory() {
        return (consensusAlgorithm, dbSource) -> {

            switch (consensusAlgorithm) {
                case "pbft":
                    return new PbftBlockStore(dbSource);
                case "ebft":
                    return new EbftBlockStore(dbSource);
                default:
            }
            throw new NotValidateException(NOT_VALID_MSG);
        };
    }

    public void shutdown() {
        grpcServer.shutdown();
    }
}
