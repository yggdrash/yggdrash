package io.yggdrash.core;

import io.yggdrash.core.exception.NotValidteException;
import org.apache.commons.codec.binary.Hex;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockChainTests {
    private static final Logger log = LoggerFactory.getLogger(BlockChainTests.class);

    BlockGenerator blockGenerator;

    @Before
    public void setUp() {
        blockGenerator = new BlockGenerator();
    }

    @Test
    public void hash로_블록_가져오기() throws IOException {
        BlockChain blockChain = instantBlockchain();
        Block b0 = blockChain.getGenesisBlock();
        String blockHash = b0.getBlockHash();
        log.debug("Block hashString : "+ blockHash);
        Block foundBlock = blockChain.getBlockByHash(blockHash);

        assertThat(foundBlock.getBlockHash()).isEqualTo(blockHash);
    }

    @Test
    public void Index로_블록_가져오기() throws IOException {
        BlockChain blockChain = instantBlockchain();
        Block prevBlock = blockChain.getPrevBlock();
        String hash = Hex.encodeHexString(prevBlock.getHeader().getPrevBlockHash());
        assertThat(blockChain.getBlockByIndex(0L)).isEqualTo(blockChain.getGenesisBlock());
        assertThat(blockChain.getBlockByIndex(2L)).isEqualTo(prevBlock);
        assertThat(blockChain.getBlockByIndex(1L)).isEqualTo(blockChain.getBlockByHash(hash));
    }

    @Test
    public void 블록체인_검증() throws IOException {
        BlockChain blockChain = instantBlockchain();
        assertThat(blockChain.isValidChain()).isEqualTo(true);
    }

    @Test
    public void 블록체인_블록_추가시_검증() throws IOException, NotValidteException {
        BlockChain blockChain = instantBlockchain();
        Account anotherAuth = new Account();
        blockChain.addBlock(new Block(anotherAuth, blockChain.getPrevBlock(), new Transactions("6")));
        assertThat(blockChain.size()).isEqualTo(4);
    }

    @Test
    public void 블록체인에_블록_추가() throws IOException, NotValidteException {
        Account author = new Account();
        BlockChain blockChain = new BlockChain();
        Block genesisBlock = blockGenerator.generate(author, blockChain, new Transactions("0"));
        blockChain.addBlock(genesisBlock);
        assertThat(blockChain.size()).isEqualTo(1);
    }

    private BlockChain instantBlockchain() throws IOException {
        Account author = new Account();
        BlockChain blockChain = new BlockChain();
        Block b0 = new Block(author, null, new Transactions("0"));
        try {
            blockChain.addBlock(b0);
            blockChain.addBlock(new Block(author, blockChain.getPrevBlock(), new Transactions("1")));
            blockChain.addBlock(new Block(author, blockChain.getPrevBlock(), new Transactions("2")));
        } catch (NotValidteException e) {
            e.printStackTrace();
            log.warn("invalid block....");
        }
        return blockChain;
    }
}
