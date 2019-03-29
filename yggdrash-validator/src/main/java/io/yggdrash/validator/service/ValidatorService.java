package io.yggdrash.validator.service;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.config.Consensus;
import io.yggdrash.validator.data.ConsensusBlockChain;
import io.yggdrash.validator.data.ebft.EbftBlockChain;
import io.yggdrash.validator.data.pbft.PbftBlockChain;
import io.yggdrash.validator.service.ebft.EbftServerStub;
import io.yggdrash.validator.service.ebft.EbftService;
import io.yggdrash.validator.service.node.NodeServerStub;
import io.yggdrash.validator.service.pbft.PbftServerStub;
import io.yggdrash.validator.service.pbft.PbftService;
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
        this.host = defaultConfig.getString("yggdrash.validator.host");
        this.port = defaultConfig.getInt("yggdrash.validator.port");
        this.wallet = new Wallet(defaultConfig.getString("yggdrash.validator.key.path"),
                defaultConfig.getString("yggdrash.validator.key.password"));
        this.consensus = new Consensus(genesisBlock);
        this.genesisBlock = genesisBlock;
        this.taskScheduler = threadPoolTaskScheduler();
        this.blockChain = consensusBlockChain();

        switch (consensus.getAlgorithm()) {
            case "pbft":
                consensusService = new PbftService(wallet, blockChain, defaultConfig, host, port);
                taskScheduler.schedule(consensusService, new CronTrigger(consensus.getPeriod()));
                try {
                    this.grpcServer = ServerBuilder.forPort(port)
                            .addService(new PbftServerStub(blockChain, consensusService))
                            .addService(new NodeServerStub(blockChain))
                            .build()
                            .start();
                } catch (IOException e) {
                    throw new NotValidateException("Grpc IOException");
                }
                break;
            case "ebft":
                consensusService = new EbftService(wallet, blockChain, defaultConfig, host, port);
                taskScheduler.schedule(consensusService, new CronTrigger(consensus.getPeriod()));
                try {
                    this.grpcServer = ServerBuilder.forPort(port)
                            .addService(new EbftServerStub(blockChain, consensusService))
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

    private ConsensusBlockChain consensusBlockChain() {
        String algorithm = consensus.getAlgorithm();
        String dbPath = defaultConfig.getDatabasePath();
        String host = this.host;
        int port = this.port;
        String chain = genesisBlock.getChainHex();

        String keyStorePath = host + "_" + port + File.separator + chain + File.separator + algorithm + "Key";
        String blockStorePath = host + "_" + port + File.separator + chain + File.separator + algorithm + "Block";
        String txStorePath = host + "_" + port + File.separator + chain + File.separator + algorithm + "Tx";

        switch (algorithm) {
            case "pbft":
                return new PbftBlockChain(genesisBlock, dbPath, keyStorePath, blockStorePath,
                        txStorePath);
            case "ebft":
                return new EbftBlockChain(genesisBlock, dbPath, keyStorePath, blockStorePath,
                        txStorePath);

            default:
                throw new NotValidateException("Algorithm is not valid.");
        }
    }

    public void shutdown() {
        grpcServer.shutdown();
    }
}
