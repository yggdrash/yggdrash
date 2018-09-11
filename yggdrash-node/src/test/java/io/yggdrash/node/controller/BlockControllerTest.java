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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(BlockController.class)
@IfProfileValue(name = "spring.profiles.active", value = "ci")
public class BlockControllerTest {
    @Autowired
    private MockMvc mockMvc;

    private JacksonTester<BlockDto> json;

    @Before
    public void setUp() {
        JacksonTester.initFields(this, new ObjectMapper());
    }

    @Test
    @DirtiesContext
    public void shouldGetBlockByIndex() throws Exception {
        MockHttpServletResponse postResponse = mockMvc.perform(post("/blocks"))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        String contentAsString = postResponse.getContentAsString();
        long index = json.parseObject(contentAsString).getIndex();

        mockMvc.perform(get("/blocks/" + index))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(contentAsString));
    }

    @Test
    public void shouldGetBlockByHash() throws Exception {
        MockHttpServletResponse postResponse = mockMvc.perform(post("/blocks"))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        String contentAsString = postResponse.getContentAsString();
        String blockHash = json.parseObject(contentAsString).getHash();

        mockMvc.perform(get("/blocks/" + blockHash))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().getResponse();

        assertThat(postResponse.getContentAsString()).contains(blockHash);
    }

    @Test
    public void shouldGetAllBlocks() throws Exception {
        mockMvc.perform(get("/blocks")).andDo(print())
                .andExpect(status().isOk());
    }
}