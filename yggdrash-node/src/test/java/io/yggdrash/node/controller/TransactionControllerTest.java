/*
 * Copyright 2018 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.yggdrash.node.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.yggdrash.node.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(TransactionController.class)
@IfProfileValue(name = "spring.profiles.active", value = "ci")
public class TransactionControllerTest {

    private static final String BRANCH_ID = Hex.toHexString(TestUtils.STEM_CHAIN);

    @Autowired
    private MockMvc mockMvc;
    private JacksonTester<TransactionDto> json;

    @Before
    public void setUp() {
        JacksonTester.initFields(this, new ObjectMapper());
    }

    @Test
    public void shouldGetTransactionByHash() throws Exception {

        // 트랜잭션 풀에 있는 트랜잭션을 조회 후 블록 내 트랜잭션 조회 로직 추가 필요.
        TransactionDto req = TransactionDto.createBy(TestUtils.createTxHusk());

        MockHttpServletResponse postResponse = mockMvc.perform(post("/txs")
                .contentType(MediaType.APPLICATION_JSON).content(json.write(req).getJson()))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn().getResponse();

        assertThat(postResponse.getContentAsString()).contains("params");
        String postTxHash = json.parseObject(postResponse.getContentAsString()).getTxHash();

        String reqUrl = String.format("/txs/%s/%s", BRANCH_ID, postTxHash);
        MockHttpServletResponse getResponse = mockMvc.perform(get(reqUrl))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn().getResponse();

        assertThat(postResponse.getContentAsString()).isEqualTo(getResponse.getContentAsString());
    }

    @Test
    public void shouldGetAllTxs() throws Exception {
        mockMvc.perform(get("/txs/" + BRANCH_ID)).andDo(print())
                .andExpect(status().isOk());
    }

}
