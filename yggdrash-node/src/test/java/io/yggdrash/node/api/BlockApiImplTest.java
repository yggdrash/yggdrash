/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.core.consensus.Block;
import io.yggdrash.gateway.dto.BlockDto;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;

import static io.yggdrash.node.api.JsonRpcConfig.BLOCK_API;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class BlockApiImplTest {

    private static final Logger log = LoggerFactory.getLogger(BlockApiImplTest.class);
    private final String branchId = TestConstants.yggdrash().toString();

    @Test
    public void blockApiIsNotNull() {
        assertThat(BLOCK_API).isNotNull();
    }

    @Test
    public void blockNumberTest() {
        try {
            assertThat(BLOCK_API.blockNumber(branchId)).isNotNull();
        } catch (Exception exception) {
            log.debug("blockNumberTest :: exception : " + exception);
        }
    }

    @Test
    public void getBlockByHashTest() {
        try {
            assertThat(BLOCK_API.getBlockByHash(branchId,
                    "ad7dd0552336ebf3b2f4f648c4a87d7c35ed74382219e2954047ad9138a247c5",
                    true)).isNotNull();
        } catch (Exception exception) {
            log.debug("getBlockByHashTest :: exception : " + exception);
        }
    }

    @Test
    public void getBlockByNumberTest() {
        try {
            assertThat(BLOCK_API.getBlockByNumber(branchId, 0, true)).isNotNull();
        } catch (Exception exception) {
            log.debug("getBlockByNumberTest :: exception : " + exception);
        }
    }

    @Test
    public void newBlockFilter() {
        try {
            assertThat(BLOCK_API.newBlockFilter()).isZero();
        } catch (Exception exception) {
            log.debug("newBlockFilter :: exception : " + exception);
        }
    }

    @Test
    public void BlockDtoTest() throws IOException {
        // Create Block
        Block block = BlockChainTestUtils.genesisBlock();

        ObjectMapper mapper = new ObjectMapper();
        String jsonStr = mapper.writeValueAsString(BlockDto.createBy(block));

        // Receive Transaction
        BlockDto resDto = mapper.readValue(jsonStr, BlockDto.class);

        assertEquals(Hex.toHexString(block.getSignature()), resDto.signature);
    }

}
