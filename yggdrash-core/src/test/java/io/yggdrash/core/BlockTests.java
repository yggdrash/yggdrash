package io.yggdrash.core;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class BlockTests {
    private BlockGenerator blockGenerator;

    @Before
    public void setUp() {
        blockGenerator = new BlockGenerator();
    }

    @Test
    public void 다음_블록_생성() {
        blockGenerator.generate("genesis", getStaticTime());
        Block b2 = blockGenerator.generate("second", getStaticTime()+10*1000);
        assertThat(b2.hash)
                .isEqualTo("d1561a4c3174eb99777e9d68179f67bdb7e39f772180f9004fe50fe377ea7830");
    }

    @Test
    public void 최초_블록_생성() {
        // https://www.fileformat.info/tool/hash.htm
        Block block = blockGenerator.generate("genesis", getStaticTime());
        String blockHash = block.hash;
        assertThat(blockHash)
                .isEqualTo("e428e2e7e18314d6b11f0e94967a14dec83af1a4c8b6681efd88329b9bca14db");
    }

    private Long getStaticTime() {
        LocalDateTime ldt = LocalDateTime.of(2018, 5, 4, 12, 0, 0);
        ZonedDateTime zdt = ldt.atZone(ZoneId.of("UTC"));
        return zdt.toInstant().toEpochMilli();
    }
}
