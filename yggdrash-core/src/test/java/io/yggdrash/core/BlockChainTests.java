package io.yggdrash.core;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockChainTests {
    BlockGenerator blockGenerator;

    @Before
    public void setUp() {
        blockGenerator = new BlockGenerator();
    }

    @Test
    public void 블록체인_블록_추가시_검증() {
        Block b1 = blockGenerator.generate("0");
        Block b2 = blockGenerator.generate("1");
        BlockChain blockChain = new BlockChain();
        blockChain.addBlock(b1);
        blockChain.addBlock(b2);
        assertThat(blockChain.size()).isEqualTo(2);

        //블록인덱스 이전보다 하나 큰 것
        Block invalidIndexBlock = new Block(3L, b2.hash, System.currentTimeMillis(),
                "invalid Index");
        blockChain.addBlock(invalidIndexBlock);
        assertThat(blockChain.size()).isEqualTo(2);

        //이전 블록 해시 일치
        Block invalidPrevHashBlock = new Block(2L, "00x0",
                System.currentTimeMillis(), "invalid Previous Hash");
        blockChain.addBlock(invalidPrevHashBlock);
        assertThat(blockChain.size()).isEqualTo(2);

        //블록 Hash 값 유효
        Block validBlock = new Block(2L, b2.hash, System.currentTimeMillis(),
                "valid Index");
        validBlock.setData("changed data");
        blockChain.addBlock(validBlock);
        assertThat(blockChain.size()).isEqualTo(2);
    }

    @Test
    public void 블록체인에_블록_추가() {
        Block genesisBlock = blockGenerator.generate("genesis");
        BlockChain blockChain = new BlockChain();
        blockChain.addBlock(genesisBlock);
        assertThat(blockChain.size()).isEqualTo(1);
    }
}
