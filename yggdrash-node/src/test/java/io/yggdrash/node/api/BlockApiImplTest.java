package io.yggdrash.node.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.yggdrash.TestUtils;
import io.yggdrash.core.BlockHusk;
import io.yggdrash.core.BranchId;
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
    private final String branchId = BranchId.STEM;

    @Test
    public void blockApiIsNotNull() {
        assertThat(blockApi).isNotNull();
    }

    @Test
    public void blockNumberTest() {
        try {
            assertThat(blockApi.blockNumber(branchId)).isNotNull();
        } catch (Exception exception) {
            log.debug("blockNumberTest :: exception : " + exception);
        }
    }

    @Test
    public void getBlockByHashTest() {
        try {
            //String hashOfBlock = blockApi.getBlockByHash(branchId,"0", true).getPrevHash().toString();
            assertThat(blockApi.getBlockByHash(branchId, "ad7dd0552336ebf3b2f4f648c4a87d7c35ed74382219e2954047ad9138a247c5", true)).isNotNull();
        } catch (Exception exception) {
            log.debug("getBlockByHashTest :: exception : " + exception);
        }
    }

    @Test
    public void getBlockByNumberTest() {
        try {
            assertThat(blockApi.getBlockByNumber(branchId, 0, true)).isNotNull();
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
