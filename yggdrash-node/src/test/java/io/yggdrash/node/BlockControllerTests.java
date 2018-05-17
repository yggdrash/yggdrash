package io.yggdrash.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@WebMvcTest(BlockController.class)
public class BlockControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BlockBuilder blockBuilder;

    @Autowired
    private BlockChain blockChain;

    private JacksonTester<BlockDto> json;

    @Before
    public void setUp() {
        JacksonTester.initFields(this, new ObjectMapper());
    }

    @Test
    @DirtiesContext
    public void 인덱스로_블록_조회() throws Exception {
        MockHttpServletResponse postResponse = mockMvc.perform(post("/blocks")
                .contentType(MediaType.APPLICATION_JSON).content("queryByIndex"))
                .andReturn().getResponse();

        MockHttpServletResponse getResponse = mockMvc.perform(get("/blocks/" + 0))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().getResponse();

        assertThat(postResponse.getContentAsString()).isEqualTo(getResponse.getContentAsString());
    }

    @Test
    public void 해쉬로_블록_조회() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/blocks"))
                .andExpect(status().isOk())
                .andReturn();

        String contentAsString = mvcResult.getResponse().getContentAsString();
        String hash = json.parseObject(contentAsString).getHash();

        mockMvc.perform(get("/blocks/" + hash))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(contentAsString));
    }

    @Test
    public void 블록_전체_조회() throws Exception {
        mockMvc.perform(post("/blocks")
                .contentType(MediaType.APPLICATION_JSON).content("0"));
        mockMvc.perform(post("/blocks")
                .contentType(MediaType.APPLICATION_JSON).content("1"));
        mockMvc.perform(post("/blocks")
                .contentType(MediaType.APPLICATION_JSON).content("2"));

        mockMvc.perform(get("/blocks")).andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void 블록이_생성되어야_한다() throws Exception {
        mockMvc.perform(post("/blocks"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("@.index").value(0));
    }
}