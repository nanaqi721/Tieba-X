package com.mint.ai.controller;

import com.mint.ai.common.vo.PostSearchItemVO;
import com.mint.ai.common.vo.PostSearchResultVO;
import com.mint.ai.common.exception.ClientException;
import com.mint.ai.handler.GlobalExceptionHandler;
import com.mint.ai.service.PostFavoriteService;
import com.mint.ai.service.PostLikeService;
import com.mint.ai.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PostSearchControllerTest {

    @Mock
    private PostService postService;

    @Mock
    private PostLikeService postLikeService;

    @Mock
    private PostFavoriteService postFavoriteService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PostController controller = new PostController(postService, postLikeService, postFavoriteService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void searchPosts_whenRequestIsValid_shouldReturnSearchPage() throws Exception {
        LocalDateTime createTime = LocalDateTime.of(2026, 8, 20, 10, 30);
        when(postService.searchPosts("Java", null, 10)).thenReturn(PostSearchResultVO.builder()
                .records(List.of(PostSearchItemVO.builder()
                        .postId("2002")
                        .barId("1001")
                        .barName("Java吧")
                        .barAvatarUrl("java.png")
                        .title("Java 学习记录")
                        .content("正文")
                        .createTime(createTime)
                        .build()))
                .nextCursor(null)
                .hasMore(false)
                .build());

        mockMvc.perform(get("/api/posts/v1/search")
                        .param("keyword", "Java")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.records[0].postId").value("2002"))
                .andExpect(jsonPath("$.data.records[0].barName").value("Java吧"))
                .andExpect(jsonPath("$.data.records[0].title").value("Java 学习记录"))
                .andExpect(jsonPath("$.data.records[0].content").value("正文"))
                .andExpect(jsonPath("$.data.hasMore").value(false))
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist());
    }

    @Test
    void searchPosts_whenRequestIsInvalid_shouldReturnUserError() throws Exception {
        when(postService.searchPosts(" ", null, null)).thenThrow(new ClientException("搜索词不能为空"));

        mockMvc.perform(get("/api/posts/v1/search").param("keyword", " "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0001"))
                .andExpect(jsonPath("$.message").value("搜索词不能为空"));
    }

    @Test
    void searchPosts_whenKeywordIsMissing_shouldReturnUserError() throws Exception {
        when(postService.searchPosts(null, null, null)).thenThrow(new ClientException("搜索词不能为空"));

        mockMvc.perform(get("/api/posts/v1/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0001"));
    }
}
