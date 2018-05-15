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
    public void hash로_블록_가져오기() throws IOException, NotValidteException {
        BlockChain blockChain = instanteBlockchain();
        Block b0 = blockChain.getGenesisBlock();
        log.debug("Block hashString : "+b0.getHeader().hashString());
        String b0Hash = b0.getHeader().hashString();
        Block foundBlock = blockChain.getBlockByHash(b0Hash);

        assert foundBlock.getHeader().hashString().equals(b0Hash);
    }

    @Test
    public void Index로_블록_가져오기() throws IOException, NotValidteException {
        BlockChain blockChain = instanteBlockchain();
        Block prev = blockChain.getPrevBlock();
        String hash = Hex.encodeHexString(prev.getHeader().getPre_block_hash());
        assertThat(blockChain.getBlockByIndex(0L)).isEqualTo(blockChain.getGenesisBlock());
        assertThat(blockChain.getBlockByIndex(2L)).isEqualTo(prev);
        assertThat(blockChain.getBlockByIndex(1L)).isEqualTo(blockChain.getBlockByHash(hash));
    }

    @Test
    public void 블록체인_검증() throws IOException, NotValidteException {
        BlockChain blockChain = instanteBlockchain();
        assertThat(blockChain.isValidChain()).isEqualTo(true);
    }

    @Test
    public void 블록체인_블록_추가시_검증() throws IOException, NotValidteException {
        BlockChain blockChain = instanteBlockchain();
        Account anotherAuth = new Account();
        blockChain.addBlock(new Block(anotherAuth, blockChain.getPrevBlock(), new Transactions("6")));
        assertThat(blockChain.size()).isEqualTo(4);
    }

    @Test
    public void TransactionGenTest() throws IOException, NotValidteException {
        // 모든 테스트는 독립적으로 동작 해야 합니다
        BlockChain blockchain = instanteBlockchain();
        int testBlock = 100;
        Account author = new Account();
        // create blockchain with genesis block
        for(int i=0; i < testBlock; i++) {
            // create next block
            Block block = new Block(author, blockchain.getPrevBlock(), new Transactions(""));
            log.debug(""+block.getHeader().getIndex());
            if(blockchain.getPrevBlock() != null) {
                log.debug("chain prev block hash : "+blockchain.getPrevBlock().getHeader().hashString());

            }
            assert block.getHeader().getIndex() == i+3;
            // add next block in blockchain
            blockchain.addBlock(block);
        }

        assert blockchain.size() == testBlock+3;

    }

    private BlockChain instanteBlockchain() throws IOException, NotValidteException {
        Account author = new Account();
        BlockChain blockChain = new BlockChain();
        Block b0 = new Block(author, null, new Transactions("0"));
        blockChain.addBlock(b0);
        blockChain.addBlock(new Block(author, blockChain.getPrevBlock(), new Transactions("1")));
        blockChain.addBlock(new Block(author, blockChain.getPrevBlock(), new Transactions("2")));
        return blockChain;
    }


}
