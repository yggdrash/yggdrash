package io.yggdrash.core;

import io.yggdrash.TestUtils;
import io.yggdrash.config.Constants;
import io.yggdrash.config.DefaultConfig;
import io.yggdrash.core.store.BlockStore;
import io.yggdrash.core.store.datasource.HashMapDbSource;
import io.yggdrash.util.FileUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockChainTest {
    private static final Logger log = LoggerFactory.getLogger(BlockChainTest.class);
    private static Wallet wallet;
    private static DefaultConfig defaultConfig;
    private String chainId = "chainId";

    @BeforeClass
    public static void init() throws Exception {
        defaultConfig = new DefaultConfig();
        wallet = new Wallet(defaultConfig);
    }

    @Test
    public void shouldBeGetBlockByHash() {
        BlockChain blockChain = instantBlockchain();
        BlockHusk b0 = blockChain.getGenesisBlock();
        String blockHash = b0.getHash().toString();
        log.debug("Block hashString : " + blockHash);
        BlockHusk foundBlock = blockChain.getBlockByHash(blockHash);

        assertThat(foundBlock.getHash()).isEqualTo(b0.getHash());
    }

    @Test
    public void shouldBeGetBlockByIndex() {
        BlockChain blockChain = instantBlockchain();
        log.debug(blockChain.toStringStatus());

        BlockHusk prevBlock = blockChain.getPrevBlock();
        String hash = prevBlock.getPrevBlockHash();
        assertThat(blockChain.getBlockByIndex(0L)).isEqualTo(blockChain.getGenesisBlock());
        assertThat(blockChain.getBlockByIndex(3L)).isEqualTo(prevBlock);
        assertThat(blockChain.getBlockByIndex(2L)).isEqualTo(blockChain.getBlockByHash(hash));
    }

    @Test
    public void shouldBeVerifiedBlockChain() {
        BlockChain blockChain = instantBlockchain();
        assertThat(blockChain.isValidChain()).isEqualTo(true);
    }

    @Test
    public void TransactionGenTest() {
        // 모든 테스트는 독립적으로 동작 해야 합니다
        BlockChain blockchain = instantBlockchain();
        int testBlock = 100;

        TransactionHusk tx = TestUtils.createTxHusk();
        for (int i = 0; i < testBlock; i++) {
            BlockHusk block = BlockHusk.build(wallet, Collections.singletonList(tx),
                    blockchain.getPrevBlock());
            assert block.getIndex() == i + 4;
            // add next block in blockchain
            blockchain.addBlock(block);
        }

        assert blockchain.size() == testBlock + 4;
    }

    @Test
    public void shouldBeLoadedStoredBlocks() {
        BlockChain blockChain = new BlockChain(chainId);
        TransactionHusk tx = TestUtils.createTxHusk();

        BlockHusk testBlock = BlockHusk.build(wallet, Collections.singletonList(tx),
                blockChain.getPrevBlock());
        blockChain.addBlock(testBlock);
        blockChain.close();

        BlockChain otherBlockChain = new BlockChain(chainId);
        BlockHusk foundBlock = otherBlockChain.getBlockByHash(testBlock.getHash());
        assertThat(otherBlockChain.size()).isEqualTo(3);
        assertThat(testBlock).isEqualTo(foundBlock);
        clearTestDb();
    }

    @Test
    public void shouldBeCreatedNewBlockChain() {
        new BlockChain(chainId);
        clearTestDb();
    }

    private BlockChain instantBlockchain() {
        BlockStore blockStore = new BlockStore(new HashMapDbSource());
        BlockChain blockChain = new BlockChain(blockStore);
        TransactionHusk tx = TestUtils.createTxHusk();

        BlockHusk block = BlockHusk.build(wallet, Collections.singletonList(tx),
                blockChain.getPrevBlock());
        blockChain.addBlock(block);

        BlockHusk newBlock =
                BlockHusk.build(wallet, Collections.singletonList(tx), blockChain.getPrevBlock());
        blockChain.addBlock(newBlock);

        newBlock =
                BlockHusk.build(wallet, Collections.singletonList(tx), blockChain.getPrevBlock());
        blockChain.addBlock(newBlock);

        return blockChain;
    }

    private void clearTestDb() {
        String dbPath = defaultConfig.getConfig().getString(Constants.DATABASE_PATH);
        FileUtil.recursiveDelete(Paths.get(dbPath, chainId));
    }
}
