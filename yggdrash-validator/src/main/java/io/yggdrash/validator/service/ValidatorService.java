package io.yggdrash.validator.service;

import ch.qos.logback.classic.Level;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.yggdrash.common.config.Constants;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.consensus.Consensus;
import io.yggdrash.core.consensus.ConsensusBlockChain;
import io.yggdrash.core.consensus.ConsensusService;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.data.ebft.EbftBlockChain;
import io.yggdrash.validator.data.pbft.PbftBlockChain;
import io.yggdrash.validator.service.ebft.EbftServerStub;
import io.yggdrash.validator.service.ebft.EbftService;
import io.yggdrash.validator.service.node.TransactionServiceStub;
import io.yggdrash.validator.service.pbft.PbftServerStub;
import io.yggdrash.validator.service.pbft.PbftService;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.io.File;
import java.io.IOException;

public class ValidatorService {

    private final DefaultConfig defaultConfig;
    private final Consensus consensus;

    private final String host;
    private final int port;
    private final Wallet wallet;
    private final Block genesisBlock;

    private ThreadPoolTaskScheduler taskScheduler;

    private final ConsensusBlockChain blockChain;
    private final ConsensusService consensusService;
    private final Server grpcServer;

    public ValidatorService(
            DefaultConfig defaultConfig, Block genesisBlock) throws IOException, InvalidCipherTextException {
        this.defaultConfig = defaultConfig;
        setLogLevel();
        this.host = defaultConfig.getString(Constants.VALIDATOR_GRPC_HOST_CONF);
        this.port = defaultConfig.getInt(Constants.VALIDATOR_GRPC_PORT_CONF);
        this.wallet = new Wallet(defaultConfig.getString(Constants.YGGDRASH_KEY_PATH),
                defaultConfig.getString(Constants.YGGDRASH_KEY_PASSWORD));
        this.consensus = new Consensus(genesisBlock);
        this.genesisBlock = genesisBlock;
        this.taskScheduler = threadPoolTaskScheduler();
        this.blockChain = consensusBlockChain();

        switch (consensus.getAlgorithm()) {
            case "pbft":
                this.consensusService = new PbftService(wallet, blockChain, defaultConfig, host, port);
                taskScheduler.schedule(consensusService, new CronTrigger(consensus.getPeriod()));
                try {
                    this.grpcServer = ServerBuilder.forPort(port)
                            .addService(new PbftServerStub((PbftService) consensusService))
                            .addService(new TransactionServiceStub(blockChain, consensusService))
                            .build()
                            .start();
                } catch (IOException e) {
                    throw new NotValidateException("Grpc IOException");
                }
                break;
            case "ebft":
                this.consensusService = new EbftService(wallet, blockChain, defaultConfig, host, port);
                taskScheduler.schedule(consensusService, new CronTrigger(consensus.getPeriod()));
                try {
                    this.grpcServer = ServerBuilder.forPort(port)
                            .addService(new EbftServerStub((EbftService) consensusService))
                            .addService(new TransactionServiceStub(blockChain, consensusService))
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

    private void setLogLevel() {
        String logLevel = this.defaultConfig.getString(Constants.VALIDATOR_LOG_LEVEL_CONF);
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

    private ConsensusBlockChain consensusBlockChain() {
        String algorithm = consensus.getAlgorithm();
        String dbPath = defaultConfig.getDatabasePath();
        String chain = genesisBlock.getBranchId().toString();

        String keyStorePath = host + "_" + port + File.separator + chain + File.separator + algorithm + "Key";
        String blockStorePath = host + "_" + port + File.separator + chain + File.separator + algorithm + "Block";

        switch (algorithm) {
            case "pbft":
                return new PbftBlockChain(genesisBlock, dbPath, keyStorePath, blockStorePath);
            case "ebft":
                return new EbftBlockChain(genesisBlock, dbPath, keyStorePath, blockStorePath);

            default:
                throw new NotValidateException("Algorithm is not valid.");
        }
    }

    public void shutdown() {
        grpcServer.shutdown();
    }
}
