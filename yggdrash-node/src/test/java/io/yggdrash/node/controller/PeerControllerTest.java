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
import io.yggdrash.core.BranchId;
import io.yggdrash.core.net.Peer;
import io.yggdrash.core.net.PeerGroup;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(PeerController.class)
@IfProfileValue(name = "spring.profiles.active", value = "ci")
public class PeerControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PeerGroup peerGroup;

    @Before
    public void setUp() {
        JacksonTester.initFields(this, new ObjectMapper());
    }

    @Test
    public void shouldGetPeers() throws Exception {
        peerGroup.addPeer(BranchId.yeed(), Peer.valueOf("ynode://75bff16c@127.0.0.1:32918"));

        mockMvc
                .perform(
                        get("/peers?branchId=" + BranchId.stem().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andDo(print());

        mockMvc
                .perform(
                        get("/peers?branchId=" + BranchId.yeed().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andDo(print());
    }

    @Test
    public void shouldGetActivePeers() throws Exception {
        mockMvc
                .perform(
                        get("/peers/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)))
                .andDo(print());
    }
}
