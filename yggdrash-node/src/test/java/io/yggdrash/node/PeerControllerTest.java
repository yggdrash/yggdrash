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

package io.yggdrash.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(PeerController.class)
@Import(TestConfig.class)
public class PeerControllerTest {
    @Autowired
    private MockMvc mockMvc;

    private JacksonTester<PeerDto> json;

    @Before
    public void setUp() {
        JacksonTester.initFields(this, new ObjectMapper());
    }

    @Test
    public void 피어가_추가되어야_한다() throws Exception {
        requestPeerPost(new PeerDto("127.0.0.1", 8080))
                .andDo(print())
                .andExpect(jsonPath("$.host", equalTo("127.0.0.1")))
                .andExpect(jsonPath("$.port", equalTo(8080)));
    }

    @Test
    public void 피어목록이_조회되어야_한다() throws Exception {
        requestPeerPost(new PeerDto("127.0.0.1", 8080));
        requestPeerPost(new PeerDto("30.30.30.30", 8080));

        mockMvc
                .perform(
                        get("/peers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andDo(print());
    }

    private ResultActions requestPeerPost(PeerDto peerDto) throws Exception {
        return mockMvc
                .perform(
                        post("/peers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.write(peerDto).getJson()))
                .andExpect(status().isOk());
    }
}
