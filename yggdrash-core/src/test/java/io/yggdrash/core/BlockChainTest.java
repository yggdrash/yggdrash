package io.yggdrash.core;

import io.yggdrash.TestUtils;
import io.yggdrash.config.Constants;
import io.yggdrash.config.DefaultConfig;
import io.yggdrash.core.store.BlockStore;
import io.yggdrash.core.store.datasource.HashMapDbSource;
import io.yggdrash.util.FileUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockChainTest {
    private static final Logger log = LoggerFactory.getLogger(BlockChainTest.class);
    private static Wallet wallet;
    private static DefaultConfig defaultConfig;
    private String chainId = "chainId";
    private File sampleBranchInfo;

    @Before
    public void init() throws Exception {
        defaultConfig = new DefaultConfig();
        wallet = new Wallet(defaultConfig);
        sampleBranchInfo = new File(Objects.requireNonNull(getClass().getClassLoader()
                .getResource("branch-sample.json")).getFile());
    }

    @After
    public void tearDown() throws Exception {
        clearTestDb();
    }

    @Test
    public void shouldBeGetBlockByHash() {
        BlockChain blockChain = instantBlockchain();
        BlockHusk b0 = blockChain.getPrevBlock();
        blockChain.addBlock(b0);

        String blockHash = b0.getHash().toString();
        log.debug("Block hashString : " + blockHash);
        BlockHusk foundBlock = blockChain.getBlockByHash(blockHash);

        assertThat(foundBlock.getHash()).isEqualTo(b0.getHash());
    }

    @Test
    public void shouldBeGetBlockByIndex() {
        BlockChain blockChain = instantBlockchain();
        log.debug(blockChain.toStringStatus());
        BlockHusk prevBlock = blockChain.getPrevBlock(); // goto Genesis
        BlockHusk currentBlock = blockChain.getPrevBlock();
        do {
            currentBlock = prevBlock;
            prevBlock = blockChain.getBlockByHash(currentBlock.getHash());
        } while (prevBlock == null);

        String hash = currentBlock.getPrevBlockHash();
        assertThat(blockChain.getBlockByIndex(0L)).isEqualTo(blockChain.getBlockByHash(hash));

    }

    @Test
    public void shouldBeVerifiedBlockChain() {
        BlockChain blockChain = instantBlockchain();
        assertThat(blockChain.isValidChain()).isEqualTo(true);
    }

    @Test
    public void shouldBeLoadedStoredBlocks() {
        BlockChain blockChain = new BlockChain(sampleBranchInfo);
        TransactionHusk tx = TestUtils.createTxHusk();

        BlockHusk testBlock = new BlockHusk(TestUtils.getBlockFixture());
        blockChain.addBlock(testBlock);
        blockChain.close();

        BlockChain otherBlockChain = new BlockChain(sampleBranchInfo);
        BlockHusk foundBlock = otherBlockChain.getBlockByHash(testBlock.getHash());
        assertThat(otherBlockChain.size()).isEqualTo(1);
        assertThat(testBlock).isEqualTo(foundBlock);
    }

    @Test
    public void shouldBeCreatedNewBlockChain() {
        BlockChain blockChain = new BlockChain(sampleBranchInfo);
    }

    private BlockChain instantBlockchain() {
        BlockStore blockStore = new BlockStore(new HashMapDbSource());
        BlockChain blockChain = new BlockChain(sampleBranchInfo);
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
