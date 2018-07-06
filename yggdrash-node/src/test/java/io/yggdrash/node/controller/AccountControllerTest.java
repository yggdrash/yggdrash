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
import io.yggdrash.node.TestConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//@RunWith(SpringRunner.class)
//@WebMvcTest(AccountController.class)
//@Import(TestConfig.class)
public class AccountControllerTest {
//    @Autowired
    private MockMvc mockMvc;

    private JacksonTester<AccountDto> json;

//    @Before
    public void setUp() {
        JacksonTester.initFields(this, new ObjectMapper());
    }

//    @Test
    // TODO Account resful api check
    public void shouldCreateAccount() throws Exception {
        String jsonResponse = mockMvc
                .perform(
                        post("/account"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        System.out.println(jsonResponse);
        AccountDto response = json.parseObject(jsonResponse);
        assertThat(response.getAddress()).isNotEmpty();
        assertThat(response.getAddress()).hasSize(40);
        System.out.println(response.getAddress());
    }
}
