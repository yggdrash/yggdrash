package io.yggdrash.gateway.controller;

import io.yggdrash.TestConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.cloud.autoconfigure.RefreshEndpointAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(StemController.class)
@Import(RefreshEndpointAutoConfiguration.class)
@IfProfileValue(name = "spring.profiles.active", value = TestConstants.CI_TEST)
public class StemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void shouldGetBranches() throws Exception {
        mockMvc.perform(get("/stem/blocks"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().getResponse();
    }
}