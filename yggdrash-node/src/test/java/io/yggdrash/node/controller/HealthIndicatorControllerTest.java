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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(HealthIndicatorController.class)
@IfProfileValue(name = "spring.profiles.active", value = "ci")
public class HealthIndicatorControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Value("${management.endpoints.web.base-path:/actuator}")
    private String actuatorRoot;

    @Value("${management.endpoints.web.path-mapping.health:health}")
    private String healthPath;

    @Test
    public void shouldGetHealth() throws Exception {

        MockHttpServletResponse response = mockMvc.perform(get(getPath()))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn().getResponse();

        assertThat(response.getContentAsString()).contains("UP");
    }

    private String getPath() {
        return actuatorRoot + "/" + healthPath;
    }
}