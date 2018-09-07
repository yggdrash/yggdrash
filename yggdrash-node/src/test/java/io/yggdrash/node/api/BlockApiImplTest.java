package io.yggdrash.node.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.node.TestUtils;
import io.yggdrash.node.controller.BlockDto;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class BlockApiImplTest {

    private static final Logger log = LoggerFactory.getLogger(TransactionApi.class);
    private static final BlockApi blockApi = new JsonRpcConfig().blockApi();

    @Test
    public void blockApiIsNotNull() {
        assertThat(blockApi).isNotNull();
    }

    @Test
    public void blockNumberTest() {
        try {
            assertThat(blockApi.blockNumber()).isNotNull();
        } catch (Exception exception) {
            log.debug("blockNumberTest :: exception : " + exception);
        }
    }

    @Test
    public void getAllBlockTest() {
        try {
            assertThat(blockApi.getAllBlock().size()).isNotZero();
        } catch (Exception exception) {
            log.debug("getAllBlockTest :: exception : " + exception, exception);
        }
    }

    @Test
    public void getBlockByHashTest() {
        try {
            String hashOfBlock = blockApi.getBlockByHash("0", true).getPrevHash().toString();
            assertThat(blockApi.getBlockByHash(hashOfBlock, true)).isNotNull();
        } catch (Exception exception) {
            log.debug("getBlockByHashTest :: exception : " + exception);
        }
    }

    @Test
    public void getBlockByNumberTest() {
        try {
            assertThat(blockApi.getBlockByNumber("0", true)).isNotNull();
        } catch (Exception exception) {
            log.debug("getBlockByNumberTest :: exception : " + exception);
        }
    }

    @Test
    public void newBlockFilter() {
        try {
            assertThat(blockApi.newBlockFilter()).isZero();
        } catch (Exception exception) {
            log.debug("newBlockFilter :: exception : " + exception);
        }
    }


    @Test
    public void BlockDtoTest() throws IOException {
        // Create Transaction
        BlockHusk block = TestUtils.createGenesisBlockHusk();

        ObjectMapper mapper = TestUtils.getMapper();
        String jsonStr = mapper.writeValueAsString(BlockDto.createBy(block));

        // Receive Transaction
        BlockDto resDto = mapper.readValue(jsonStr, BlockDto.class);

        assertEquals(Hex.toHexString(block.getInstance().getSignature().toByteArray()),
                resDto.getSignature());
    }

}
