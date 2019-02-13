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

package io.yggdrash.gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.yggdrash.TestConstants;
import io.yggdrash.node.api.dto.BlockDto;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.cloud.autoconfigure.RefreshEndpointAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(BlockController.class)
@Import(RefreshEndpointAutoConfiguration.class)
@IfProfileValue(name = "spring.profiles.active", value = TestConstants.CI_TEST)
public class BlockControllerTest {

    private String basePath;

    @Autowired
    private MockMvc mockMvc;

    private JacksonTester<BlockDto> json;

    @Before
    public void setUp() {
        JacksonTester.initFields(this, new ObjectMapper());
        basePath = String.format("/branches/%s/blocks", TestConstants.yggdrash());
    }

    @Test
    public void shouldGetBlock() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get(basePath + "/0"))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        String contentAsString = response.getContentAsString();
        String blockId = json.parseObject(contentAsString).blockId;

        response = mockMvc.perform(get(basePath + "/" + blockId))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().getResponse();

        assertThat(response.getContentAsString()).contains(blockId);
    }

    @Test
    public void shouldGetAllBlocks() throws Exception {
        mockMvc.perform(get(basePath)).andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void shouldGetLatest() throws Exception {
        mockMvc.perform(get(basePath + "/latest")).andDo(print())
                .andExpect(status().isOk());
    }
}