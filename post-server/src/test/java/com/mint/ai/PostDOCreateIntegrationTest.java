package com.mint.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mint.ai.mapper.PostMapper;
import com.mint.ai.mapper.entiy.PostDO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 帖子创建接口集成测试
 *
 * 覆盖：正常创建（含数据库落库校验）、参数校验失败、未匹配路径 404。
 * @Transactional 保证测试事务自动回滚，数据不残留。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PostDOCreateIntegrationTest {

    /**
     * 模拟请求用的
     */
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostMapper postMapper;

    private static final String BAR_ID = "1001";

    @Test
    void createPost_whenValid_shouldReturnNewPostIdAndPersist() throws Exception {
        String body = "{\"title\":\"这是一个合法的帖子标题\",\"content\":\"这是帖子内容\"}";

        MvcResult result = mockMvc.perform(post("/api/posts/v1/{barId}/create", BAR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andReturn();

        String id = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .at("/data/id").asText();
        assertThat(id).isNotBlank();

        // 验证数据库落库（同一事务内可见）
        PostDO saved = postMapper.selectById(id);
        assertThat(saved).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("这是一个合法的帖子标题");
        assertThat(saved.getContent()).isEqualTo("这是帖子内容");
    }

    @Test
    void createPost_whenTitleBlank_shouldReturnUserError() throws Exception {
        String body = "{\"title\":\"\",\"content\":\"这是帖子内容\"}";

        mockMvc.perform(post("/api/posts/v1/{barId}/create", BAR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0001"))
                .andExpect(jsonPath("$.message", containsString("帖子标题不能为空")));
    }

    @Test
    void createPost_whenTitleTooShort_shouldReturnUserError() throws Exception {
        String body = "{\"title\":\"标题\",\"content\":\"这是帖子内容\"}";

        mockMvc.perform(post("/api/posts/v1/{barId}/create", BAR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0001"))
                .andExpect(jsonPath("$.message", containsString("帖子标题长度需在5-30之间")));
    }

    @Test
    void createPost_whenContentBlank_shouldReturnUserError() throws Exception {
        String body = "{\"title\":\"这是一个合法的帖子标题\",\"content\":\"\"}";

        mockMvc.perform(post("/api/posts/v1/{barId}/create", BAR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0001"))
                .andExpect(jsonPath("$.message", containsString("帖子内容不能为空")));
    }

    @Test
    void requestUnknownPath_shouldReturnUserError() throws Exception {
        mockMvc.perform(get("/api/posts/v1/{barId}/unknown-method", BAR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0001"))
                .andExpect(jsonPath("$.message").value("请求资源不存在"));
    }
}
