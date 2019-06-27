/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParser;
import io.yggdrash.BlockChainTestUtils;
import io.yggdrash.TestConstants;
import io.yggdrash.common.config.Constants;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.gateway.dto.TransactionDto;
import io.yggdrash.node.YggdrashNodeApp;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest(classes = YggdrashNodeApp.class)
@ActiveProfiles(Constants.ActiveProfiles.GATEWAY)
public class TransactionControllerTest extends TestConstants.CiTest {

    private String basePath;

    @Autowired
    private MockMvc mockMvc;
    private JacksonTester<TransactionDto> json;
    private BranchId yggdrashBranch;

    @Before
    public void setUp() throws Exception {
        JacksonTester.initFields(this, new ObjectMapper());
        String branchApi = "/branches";
        MvcResult result = mockMvc.perform(get(branchApi))
                .andReturn();
        JsonParser parser = new JsonParser();
        String branch = parser.parse(result.getResponse().getContentAsString())
                .getAsJsonObject()
                .keySet().iterator().next();
        yggdrashBranch = BranchId.of(branch);

        basePath = String.format("/branches/%s/txs", yggdrashBranch);
    }

    @Test
    public void shouldGetRecentTransaction() throws Exception {
        mockMvc.perform(get(basePath))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countOfTotal", is(3)))
                .andExpect(jsonPath("$.txs", hasSize(3)))
                .andExpect(jsonPath("$.txs[0].branchId", is(yggdrashBranch.toString())));
    }

    @Test
    public void shouldGetTransactionByHash() throws Exception {

        // 트랜잭션 풀에 있는 트랜잭션을 조회 후 블록 내 트랜잭션 조회 로직 추가 필요.
        TransactionDto req =
                TransactionDto.createBy(BlockChainTestUtils.createTransferTx());

        // Error Tx. Issuer has no balance!
        MockHttpServletResponse postResponse = mockMvc.perform(post(basePath)
                .contentType(MediaType.APPLICATION_JSON).content(json.write(req).getJson()))
                .andExpect(status().isBadRequest())
                .andDo(print())
                .andReturn().getResponse();

        boolean containTransfer = postResponse.getContentAsString().contains("transfer");
        assertThat(containTransfer).isFalse();

        if (containTransfer) {
            String txId = json.parseObject(postResponse.getContentAsString()).txId;

            MockHttpServletResponse getResponse = mockMvc.perform(get(basePath + "/" + txId))
                    .andExpect(status().isOk())
                    .andDo(print())
                    .andReturn().getResponse();

            assertThat(postResponse.getContentAsString()).isEqualTo(getResponse.getContentAsString());
        }
    }

    @Test
    public void shouldGetAllTxs() throws Exception {
        mockMvc.perform(get(basePath)).andDo(print()).andExpect(status().isOk());
    }

}
