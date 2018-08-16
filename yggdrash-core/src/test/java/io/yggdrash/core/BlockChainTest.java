package io.yggdrash.core;

import com.google.gson.JsonObject;
import io.yggdrash.core.exception.NotValidateException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockChainTest {
    private static final Logger log = LoggerFactory.getLogger(BlockChainTest.class);

    @Test
    public void shouldBeGetBlockByHash() throws IOException, InvalidCipherTextException {
        BlockChain blockChain = instantBlockchain();

        Block b0 = blockGenerator(blockChain.getPrevBlock());
        blockChain.addBlock(b0);

        String blockHash = b0.getBlockHash();
        log.debug("Block hashString : " + blockHash);
        Block foundBlock = blockChain.getBlockByHash(blockHash);

        assertThat(foundBlock.getBlockHash()).isEqualTo(blockHash);
    }

    @Ignore
    @Test
    public void shouldBeGetBlockByIndex() throws IOException, InvalidCipherTextException {
        // TODO 블록체인에서 모든 블록들을 다 가지고 있는 구조로 구현하면 안됩니다.
        BlockChain blockChain = instantBlockchain();
        log.debug(blockChain.toStringStatus());
        Block prevBlock = blockChain.getPrevBlock(); // goto Genesis
        Block currentBlock = blockChain.getPrevBlock();
        do {
            currentBlock = prevBlock;
            prevBlock = blockChain.getBlockByHash(currentBlock.getBlockHash());
        }while (prevBlock == null);

        String hash = currentBlock.getPrevBlockHash();
        assertThat(blockChain.getBlockByIndex(0L)).isEqualTo(blockChain.getBlockByHash(hash));
//        assertThat(blockChain.getBlockByIndex(3L)).isEqualTo(prevBlock);
//        assertThat(blockChain.getBlockByIndex(2L)).isEqualTo(blockChain.getBlockByHash(hash));
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
        for (int i = 0; i < testBlock; i++) {
            blockchain.addBlock(blockGenerator(blockchain.getPrevBlock()));
        }

        assert blockchain.size() == testBlock + 1;

    }

    private Block blockGenerator(Block prevBlock) throws IOException, InvalidCipherTextException {
        Wallet wallet = new Wallet();
        Transaction tx = new Transaction(wallet, new JsonObject());
        BlockBody sampleBody = new BlockBody(Collections.singletonList(tx));
        return new Block(new BlockHeader.Builder()
                .prevBlock(prevBlock)
                .blockBody(sampleBody).build(wallet), sampleBody);
    }

    private BlockChain instantBlockchain() throws IOException, InvalidCipherTextException {
        Wallet wallet = new Wallet();
        // @TODO load Test Genesis json
        JsonObject json = new JsonObject();


        BlockChain blockChain = new BlockChain(json);
        Transaction tx = new Transaction(wallet, new JsonObject());
        BlockBody sampleBody = new BlockBody(Collections.singletonList(tx));

        BlockHeader blockHeader = new BlockHeader.Builder()
                .blockBody(sampleBody)
                .prevBlock(blockChain.getPrevBlock())
                .build(wallet);

        Block b0 = new Block(blockHeader, sampleBody);

        try {
            blockChain.addBlock(b0);
//            blockChain.addBlock(
//                    new Block(new BlockHeader.Builder()
//                            .prevBlock(blockChain.getPrevBlock())
//                            .blockBody(sampleBody).build(wallet), sampleBody));
//            blockChain.addBlock(
//                    new Block(new BlockHeader.Builder()
//                            .prevBlock(blockChain.getPrevBlock())
//                            .blockBody(sampleBody).build(wallet), sampleBody));
        } catch (NotValidateException e) {
            log.error(e.getMessage());
            log.warn("invalid block....");
            assert false;
        }
        return blockChain;
    }
}
