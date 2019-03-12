package io.yggdrash.validator.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.yggdrash.common.config.DefaultConfig;
import io.yggdrash.core.blockchain.Block;
import io.yggdrash.core.exception.NotValidateException;
import io.yggdrash.core.wallet.Wallet;
import io.yggdrash.validator.data.ConsensusBlockChain;
import io.yggdrash.validator.data.ebft.EbftBlockChain;
import io.yggdrash.validator.data.pbft.PbftBlockChain;
import io.yggdrash.validator.service.ConsensusService;
import io.yggdrash.validator.service.ebft.EbftServerStub;
import io.yggdrash.validator.service.ebft.EbftService;
import io.yggdrash.validator.service.pbft.PbftServerStub;
import io.yggdrash.validator.service.pbft.PbftService;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import static io.yggdrash.common.config.Constants.DEFAULT_PORT;

@Configuration
public class ValidatorConfiguration {

    private Server server;

    @Bean
    String grpcHost() {
        return InetAddress.getLoopbackAddress().getHostAddress();
    }

    @Bean
    int grpcPort() {
        if (System.getProperty("grpc.port") == null) {
            return DEFAULT_PORT;
        } else {
            return Integer.parseInt(System.getProperty("grpc.port"));
        }
    }

    @Bean
    DefaultConfig defaultConfig() {
        return new DefaultConfig();
    }

    @Bean
    Wallet wallet(DefaultConfig defaultConfig) throws IOException, InvalidCipherTextException {
        return new Wallet(defaultConfig);
    }

    @Bean
    Block validatorGenesisBlock() {
        String genesisString;
        ClassPathResource cpr = new ClassPathResource("genesis/genesis.json");
        try {
            byte[] bdata = FileCopyUtils.copyToByteArray(cpr.getInputStream());
            genesisString = new String(bdata, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new NotValidateException("Error genesisFile");
        }

        return new Block(new Gson().fromJson(genesisString, JsonObject.class));
    }

    @Bean
    Consensus consensus(Block validatorGenesisblock) {
        return new Consensus(validatorGenesisblock);
    }

    @Bean
    @DependsOn( {"validatorGenesisBlock", "consensus"})
    ConsensusBlockChain consensusBlockChain(Block genesisBlock,
                                            DefaultConfig defaultConfig, Consensus consensus) {
        String algorithm = consensus.getAlgorithm();
        String dbPath = defaultConfig.getDatabasePath();
        String host = grpcHost();
        String port = Long.toString(grpcPort());
        String chain = Hex.toHexString(genesisBlock.getHeader().getChain());

        String keyStorePath = host + "_" + port + "/" + chain + "/" + algorithm + "Key";
        String blockStorePath = host + "_" + port + "/" + chain + "/" + algorithm + "Block";
        String txStorePath = host + "_" + port + "/" + chain + "/" + algorithm + "Tx";

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

    @Bean
    @DependsOn( {"consensusBlockChain", "threadPoolTaskScheduler"})
    ConsensusService consensusService(Wallet wallet, Consensus consensus,
                                      ConsensusBlockChain consensusBlockChain,
                                      ThreadPoolTaskScheduler threadPoolTaskScheduler) {
        String period = consensus.getPeriod();
        String host = grpcHost();
        int port = grpcPort();

        ConsensusService consensusService;
        switch (consensus.getAlgorithm()) {
            case "pbft":
                consensusService = new PbftService(wallet, consensusBlockChain, host, port);
                threadPoolTaskScheduler.schedule(consensusService, new CronTrigger(period));
                try {
                    this.server = ServerBuilder.forPort(grpcPort())
                            .addService(new PbftServerStub(consensusBlockChain, consensusService))
                            .build()
                            .start();
                } catch (IOException e) {
                    throw new NotValidateException("Grpc IOException");
                }
                return consensusService;

            case "ebft":
                consensusService = new EbftService(wallet, consensusBlockChain, host, port);
                threadPoolTaskScheduler.schedule(consensusService, new CronTrigger(period));
                try {
                    this.server = ServerBuilder.forPort(grpcPort())
                            .addService(new EbftServerStub(consensusBlockChain, consensusService))
                            .build()
                            .start();
                } catch (IOException e) {
                    throw new NotValidateException("Grpc IOException");
                }
                return consensusService;

            default:
                throw new NotValidateException("Algorithm is not valid.");
        }
    }

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(5);
        threadPoolTaskScheduler.setThreadNamePrefix("ThreadPoolTaskScheduler");
        return threadPoolTaskScheduler;
    }
}
