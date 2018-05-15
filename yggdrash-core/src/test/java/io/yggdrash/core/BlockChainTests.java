package io.yggdrash.core;

import com.google.gson.JsonObject;
import io.yggdrash.core.exception.NotValidteException;
import io.yggdrash.proto.Blockchain;
import io.yggdrash.util.HashUtils;
import org.apache.commons.codec.binary.Hex;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockChainTests {
    private static final Logger log = LoggerFactory.getLogger(BlockChainTests.class);

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
    public void TransactionGenTest() throws IOException, NotValidteException {
        // 모든 테스트는 독립적으로 동작 해야 합니다
        BlockChain blockchain = instantBlockchain();
        int testBlock = 100;
        Account author = new Account();
        // create blockchain with genesis block
        for(int i=0; i < testBlock; i++) {
            // create next block
            Block block = new Block(author, blockchain.getPrevBlock(), new Transactions(""));
            log.debug(""+block.getHeader().getIndex());
            if(blockchain.getPrevBlock() != null) {
                log.debug("chain prev block hash : "+blockchain.getPrevBlock().getPrevBlockHash());

            }
            assert block.getHeader().getIndex() == i+3;
            // add next block in blockchain
            blockchain.addBlock(block);
        }

        assert blockchain.size() == testBlock+3;

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
