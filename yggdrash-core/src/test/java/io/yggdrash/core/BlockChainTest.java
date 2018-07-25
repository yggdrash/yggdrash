package io.yggdrash.core;

import com.google.gson.JsonObject;
import io.yggdrash.config.DefaultConfig;
import io.yggdrash.core.exception.NotValidateException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockChainTest {
    private static final Logger log = LoggerFactory.getLogger(BlockChainTest.class);

    @Test
    public void shouldBeGetBlockByHash() throws IOException, InvalidCipherTextException {
        BlockChain blockChain = instantBlockchain();
        Block b0 = blockChain.getGenesisBlock();
        String blockHash = b0.getBlockHash();
        log.debug("Block hashString : " + blockHash);
        Block foundBlock = blockChain.getBlockByHash(blockHash);

        assertThat(foundBlock.getBlockHash()).isEqualTo(blockHash);
    }

    @Test
    public void shouldBeGetBlockByIndex() throws IOException, InvalidCipherTextException {
        BlockChain blockChain = instantBlockchain();
        Block prevBlock = blockChain.getPrevBlock();
        String hash = prevBlock.getPrevBlockHash();
        assertThat(blockChain.getBlockByIndex(0L)).isEqualTo(blockChain.getGenesisBlock());
        assertThat(blockChain.getBlockByIndex(2L)).isEqualTo(prevBlock);
        assertThat(blockChain.getBlockByIndex(1L)).isEqualTo(blockChain.getBlockByHash(hash));
    }

    @Test
    public void shouldBeVerifiedBlockChain() throws IOException, InvalidCipherTextException {
        BlockChain blockChain = instantBlockchain();
        assertThat(blockChain.isValidChain()).isEqualTo(true);
    }

    @Test
    public void TransactionGenTest() throws NotValidateException, IOException,
            InvalidCipherTextException {
        // 모든 테스트는 독립적으로 동작 해야 합니다
        BlockChain blockchain = instantBlockchain();
        int testBlock = 100;
        Wallet wallet = new Wallet(new DefaultConfig());

        // create blockchain with genesis block
        Transaction tx = new Transaction(wallet, new JsonObject());
        BlockBody sampleBody = new BlockBody(Collections.singletonList(tx));
        BlockHeader.Builder builder = new BlockHeader.Builder()
                .blockBody(sampleBody);
        BlockHeader blockHeader;
        for (int i = 0; i < testBlock; i++) {
            // create next block
            blockHeader = builder.prevBlock(blockchain.getPrevBlock()).build(wallet);
            Block block = new Block(blockHeader, sampleBody);
            log.debug("" + block.getIndex());

            if (blockchain.getPrevBlock() != null) {
                log.debug("chain prev block hash : "
                        + blockchain.getPrevBlock().getPrevBlockHash());
            }
            assert block.getIndex() == i + 3;
            // add next block in blockchain
            blockchain.addBlock(block);
        }

        assert blockchain.size() == testBlock + 3;

    }

    private BlockChain instantBlockchain() throws IOException, InvalidCipherTextException {
        Wallet wallet = new Wallet(new DefaultConfig());
        BlockChain blockChain = new BlockChain();
        Transaction tx = new Transaction(wallet, new JsonObject());
        BlockBody sampleBody = new BlockBody(Collections.singletonList(tx));

        BlockHeader blockHeader = new BlockHeader.Builder()
                .blockBody(sampleBody)
                .prevBlock(null)
                .build(wallet);

        Block b0 = new Block(blockHeader, sampleBody);

        try {
            blockChain.addBlock(b0);
            blockChain.addBlock(
                    new Block(new BlockHeader.Builder()
                            .prevBlock(blockChain.getPrevBlock())
                            .blockBody(sampleBody).build(wallet), sampleBody));
            blockChain.addBlock(
                    new Block(new BlockHeader.Builder()
                            .prevBlock(blockChain.getPrevBlock())
                            .blockBody(sampleBody).build(wallet), sampleBody));
        } catch (NotValidateException e) {
            e.printStackTrace();
            log.warn("invalid block....");
        }
        return blockChain;
    }
}
