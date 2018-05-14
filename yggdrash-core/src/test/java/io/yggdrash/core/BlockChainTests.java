package io.yggdrash.core;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockChainTests {
    BlockGenerator blockGenerator;

    @Before
    public void setUp() {
        blockGenerator = new BlockGenerator();
    }

    @Test
    public void 가장_긴_체인_선택() throws IOException {
        BlockChain blockChain = new BlockChain();
        Block b0 = blockGenerator.generate("0");
        Block b1 = blockGenerator.generate("1");
        Block b2 = blockGenerator.generate("2");
        Block b3 = blockGenerator.generate("3");
        blockChain.addBlock(b0);
        blockChain.addBlock(b1);
        blockChain.addBlock(b2);

        // 더 긴 체인
        BlockChain longerChain = new BlockChain();
        longerChain.addBlock(b0);
        longerChain.addBlock(b1);
        longerChain.addBlock(b2);
        longerChain.addBlock(b3);

        blockChain.replaceChain(longerChain);
        assertThat(blockChain.size()).isEqualTo(4);

        // 짧은 체인
        BlockChain shorterChain = new BlockChain();
        shorterChain.addBlock(b0);
        shorterChain.addBlock(b1);
        blockChain.replaceChain(shorterChain);
        assertThat(blockChain.size()).isEqualTo(4);

        // 더 길지만 조작된 체인
        BlockChain scamChain = new BlockChain();
        Block b4 = blockGenerator.generate("4");
        scamChain.addBlock(b0);
        scamChain.addBlock(b1);
        scamChain.addBlock(b2);
        scamChain.addBlock(b3);
        scamChain.addBlock(b4);
        // b4.data = "changed data";
        b2.setData("recalculate hash but... invalid previous hash");
        blockChain.replaceChain(scamChain);
        assertThat(blockChain.size()).isEqualTo(4);
    }

    @Test
    public void hash로_블록_가져오기() throws IOException {
        Block b0 = blockGenerator.generate("0");
        Block b1 = blockGenerator.generate("1");
        Block b2 = blockGenerator.generate("2");
        BlockChain blockChain = new BlockChain();
        blockChain.addBlock(b0);
        blockChain.addBlock(b1);
        blockChain.addBlock(b2);

        String b1Hash = b1.hash;
        Block foundBlock = blockChain.getBlockByHash(b1Hash);
        assertThat(foundBlock).isEqualTo(b1);
    }

    @Test
    public void Index로_블록_가져오기() throws IOException {
        Block b0 = blockGenerator.generate("0");
        Block b1 = blockGenerator.generate("1");
        Block b2 = blockGenerator.generate("2");
        BlockChain blockChain = new BlockChain();
        blockChain.addBlock(b0);
        blockChain.addBlock(b1);
        blockChain.addBlock(b2);

        assertThat(blockChain.getBlockByIndex(0)).isEqualTo(b0);
        assertThat(blockChain.getBlockByIndex(1)).isEqualTo(b1);
        assertThat(blockChain.getBlockByIndex(2)).isEqualTo(b2);
    }

    @Test
    public void 블록체인_검증() throws IOException {
        BlockChain blockChain = new BlockChain();
        Block genesisBlock = blockGenerator.generate("0");
        blockChain.addBlock(genesisBlock);
        blockChain.addBlock(blockGenerator.generate("1"));
        blockChain.addBlock(blockGenerator.generate("2"));
        assertThat(blockChain.isValidChain()).isEqualTo(true);

        // 제네시스 블록 검증
        genesisBlock.setData("changed data");
        assertThat(blockChain.isValidChain()).isEqualTo(false);

        // 중간 블록 변경 감지
        genesisBlock.setData("0");
        assertThat(blockChain.isValidChain()).isEqualTo(true);
        Block secondBlock = blockChain.getBlockByIndex(1);
        secondBlock.setData("changed data");
        assertThat(blockChain.isValidChain()).isEqualTo(false);
    }

    @Test
    public void 블록체인_블록_추가시_검증() throws IOException {
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
        validBlock.data = "changed data";
        blockChain.addBlock(validBlock);
        assertThat(blockChain.size()).isEqualTo(2);
    }

    @Test
    public void 블록체인에_블록_추가() throws IOException {
        Block genesisBlock = blockGenerator.generate("genesis");
        BlockChain blockChain = new BlockChain();
        blockChain.addBlock(genesisBlock);
        assertThat(blockChain.size()).isEqualTo(1);
    }
}
