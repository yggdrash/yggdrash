package io.yggdrash.core;

import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockTests {
    @Test
    public void 다음_블록_생성() {
        String data = "second block";
        Block previousBlock = getLatestBlock();
        Long nextIndex = previousBlock.index + 1;
        Long nextTimestamp = previousBlock.timestamp + 10 * 1000;
        Block nextBlock = new Block(nextIndex, HashUtils.sha256Hex(previousBlock.toString()),
                previousBlock.getHash(), nextTimestamp, data);
        assertThat(nextBlock).isNotNull();
        assertThat(previousBlock.getHash())
                .isEqualTo("e428e2e7e18314d6b11f0e94967a14dec83af1a4c8b6681efd88329b9bca14db");
        assertThat(nextBlock.getHash())
                .isEqualTo("c23da635daae440321ba56bedea70bb988da700dbac73657a2d0a7460b393697");
    }

    private Block getLatestBlock() {
        return createGenesisBlock();
    }

    @Test
    public void 해쉬_계산() {
        // https://www.fileformat.info/tool/hash.htm
        Block block = createGenesisBlock();
        String blockHash = block.getHash();

        assertThat(blockHash)
                .isEqualTo("e428e2e7e18314d6b11f0e94967a14dec83af1a4c8b6681efd88329b9bca14db");
    }

    private Block createGenesisBlock() {
        LocalDateTime ldt = LocalDateTime.of(2018, 5, 4, 12, 0, 0);
        ZonedDateTime zdt = ldt.atZone(ZoneId.of("UTC"));
        return new Block(0L, "", "", zdt.toInstant().toEpochMilli(), "genesis");
    }
}
