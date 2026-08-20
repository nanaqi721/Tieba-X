package com.mint.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mint.ai.bar.api.clients.BarClient;
import com.mint.ai.bar.api.vo.BarBaseVO;
import com.mint.ai.common.Result;
import com.mint.ai.common.exception.ClientException;
import com.mint.ai.common.vo.PostSearchItemVO;
import com.mint.ai.common.vo.PostSearchResultVO;
import com.mint.ai.mapper.PostMapper;
import com.mint.ai.mq.producer.UserContentChangedProducer;
import com.mint.ai.service.serviceImpl.PostServiceImpl;
import com.mint.ai.user.api.clients.UserClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceSearchTest {

    @Mock
    private PostMapper postMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private BarClient barClient;

    @Mock
    private UserClient userClient;

    @Mock
    private UserContentChangedProducer userContentChangedProducer;

    private PostService postService;

    @BeforeEach
    void setUp() {
        postService = new PostServiceImpl(
                new ObjectMapper().findAndRegisterModules(),
                postMapper,
                stringRedisTemplate,
                redissonClient,
                barClient,
                userClient,
                userContentChangedProducer
        );
    }

    @Test
    void searchPosts_whenKeywordMatches_shouldReturnPostCard() {
        LocalDateTime createTime = LocalDateTime.of(2026, 8, 20, 10, 30);
        PostSearchItemVO post = PostSearchItemVO.builder()
                .postId("2002")
                .barId("1001")
                .title("Java 学习记录")
                .content("正文")
                .createTime(createTime)
                .build();
        when(postMapper.searchPosts(eq("Java"), isNull(), eq(11))).thenReturn(List.of(post));
        when(barClient.queryBarList(List.of("1001"))).thenReturn(new Result<Map<String, BarBaseVO>>()
                .setCode(Result.SUCCESS_CODE)
                .setData(Map.of("1001", BarBaseVO.builder()
                        .name("Java吧")
                        .avatarUrl("java.png")
                        .build())));

        PostSearchResultVO result = postService.searchPosts("Java", null, null);

        assertThat(result.getRecords()).containsExactly(
                PostSearchItemVO.builder()
                        .postId("2002")
                        .barId("1001")
                        .barName("Java吧")
                        .barAvatarUrl("java.png")
                        .title("Java 学习记录")
                        .content("正文")
                        .createTime(createTime)
                        .build()
        );
        assertThat(result.getHasMore()).isFalse();
        assertThat(result.getNextCursor()).isNull();
    }

    @ParameterizedTest
    @MethodSource("invalidSearchRequests")
    void searchPosts_whenRequestIsInvalid_shouldReject(String keyword, Integer pageSize) {
        assertThatThrownBy(() -> postService.searchPosts(keyword, null, pageSize))
                .isInstanceOf(ClientException.class);
    }

    private static Stream<Arguments> invalidSearchRequests() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("   ", null),
                Arguments.of("123456789012345678901", null),
                Arguments.of("Java", 0),
                Arguments.of("Java", 11)
        );
    }

    @Test
    void searchPosts_whenMoreResultsExist_shouldReturnCursorAcceptedByNextPage() {
        LocalDateTime newest = LocalDateTime.of(2026, 8, 20, 12, 0);
        List<PostSearchItemVO> posts = IntStream.rangeClosed(1, 11)
                .mapToObj(index -> PostSearchItemVO.builder()
                        .postId(String.valueOf(index))
                        .barId("1001")
                        .title("Java " + index)
                        .content("content " + index)
                        .createTime(newest.minusMinutes(index - 1L))
                        .build())
                .toList();
        when(postMapper.searchPosts(eq("Java"), isNull(), eq(11))).thenReturn(posts);
        when(postMapper.searchPosts(eq("Java"),
                org.mockito.ArgumentMatchers.argThat(cursor ->
                        cursor != null
                                && "10".equals(cursor.getPostId())
                                && newest.minusMinutes(9).equals(cursor.getCreateTime())),
                eq(11))).thenReturn(List.of());
        when(barClient.queryBarList(List.of("1001"))).thenReturn(new Result<Map<String, BarBaseVO>>()
                .setCode(Result.SUCCESS_CODE)
                .setData(Map.of("1001", BarBaseVO.builder()
                        .name("Java吧")
                        .avatarUrl("java.png")
                        .build())));

        PostSearchResultVO firstPage = postService.searchPosts("Java", null, 10);
        PostSearchResultVO secondPage = postService.searchPosts("Java", firstPage.getNextCursor(), 10);

        assertThat(firstPage.getRecords()).hasSize(10);
        assertThat(firstPage.getHasMore()).isTrue();
        assertThat(firstPage.getNextCursor()).isNotBlank();
        assertThat(secondPage.getRecords()).isEmpty();
        assertThat(secondPage.getHasMore()).isFalse();
        assertThat(secondPage.getNextCursor()).isNull();
    }

    @Test
    void searchPosts_whenContentExceedsLimit_shouldReturnThirtyCharacterSummary() {
        String thirtyCharacters = "123456789012345678901234567890";
        List<PostSearchItemVO> posts = List.of(
                PostSearchItemVO.builder()
                        .postId("1")
                        .barId("1001")
                        .title("exact")
                        .content(thirtyCharacters)
                        .createTime(LocalDateTime.of(2026, 8, 20, 12, 0))
                        .build(),
                PostSearchItemVO.builder()
                        .postId("2")
                        .barId("1001")
                        .title("long")
                        .content(thirtyCharacters + "X")
                        .createTime(LocalDateTime.of(2026, 8, 20, 11, 0))
                        .build()
        );
        when(postMapper.searchPosts(eq("正文"), isNull(), eq(11))).thenReturn(posts);
        when(barClient.queryBarList(List.of("1001"))).thenReturn(new Result<Map<String, BarBaseVO>>()
                .setCode(Result.SUCCESS_CODE)
                .setData(Map.of("1001", BarBaseVO.builder().name("测试吧").avatarUrl("").build())));

        PostSearchResultVO result = postService.searchPosts("正文", null, null);

        assertThat(result.getRecords())
                .extracting(PostSearchItemVO::getContent)
                .containsExactly(thirtyCharacters, thirtyCharacters + "...");
    }

    @Test
    void searchPosts_whenBarServiceFails_shouldReturnUnknownBarFallback() {
        PostSearchItemVO post = PostSearchItemVO.builder()
                .postId("1")
                .barId("1001")
                .title("Java")
                .content("content")
                .createTime(LocalDateTime.of(2026, 8, 20, 12, 0))
                .build();
        when(postMapper.searchPosts(eq("Java"), isNull(), eq(11))).thenReturn(List.of(post));
        when(barClient.queryBarList(List.of("1001"))).thenThrow(new RuntimeException("bar unavailable"));

        PostSearchResultVO result = postService.searchPosts("Java", null, null);

        assertThat(result.getRecords()).singleElement().satisfies(record -> {
            assertThat(record.getBarName()).isEqualTo("未知吧");
            assertThat(record.getBarAvatarUrl()).isEmpty();
        });
    }

    @Test
    void searchPosts_whenKeywordContainsLikeCharacters_shouldTreatThemAsLiterals() {
        when(postMapper.searchPosts(eq("100!%!_!!"), isNull(), eq(11))).thenReturn(List.of());

        PostSearchResultVO result = postService.searchPosts("100%_!", null, null);

        assertThat(result.getRecords()).isEmpty();
    }

    @Test
    void searchPosts_whenCursorIsMalformed_shouldReject() {
        assertThatThrownBy(() -> postService.searchPosts("Java", "not-a-cursor", null))
                .isInstanceOf(ClientException.class)
                .hasMessage("游标无效");
    }

    @Test
    void searchPosts_whenCursorFieldsAreMissing_shouldReject() {
        String incompleteCursor = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"postId\":\"1\"}".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> postService.searchPosts("Java", incompleteCursor, null))
                .isInstanceOf(ClientException.class)
                .hasMessage("游标无效");
    }

    @Test
    void searchPosts_whenCursorBelongsToAnotherKeyword_shouldReject() {
        LocalDateTime createTime = LocalDateTime.of(2026, 8, 20, 12, 0);
        List<PostSearchItemVO> posts = IntStream.rangeClosed(1, 11)
                .mapToObj(index -> PostSearchItemVO.builder()
                        .postId(String.valueOf(index))
                        .barId("1001")
                        .title("Java " + index)
                        .content("content")
                        .createTime(createTime.minusMinutes(index - 1L))
                        .build())
                .toList();
        when(postMapper.searchPosts(eq("Java"), isNull(), eq(11))).thenReturn(posts);
        when(barClient.queryBarList(List.of("1001"))).thenReturn(new Result<Map<String, BarBaseVO>>()
                .setCode(Result.SUCCESS_CODE)
                .setData(Map.of("1001", BarBaseVO.builder().name("Java吧").avatarUrl("").build())));
        String cursor = postService.searchPosts("Java", null, null).getNextCursor();

        assertThatThrownBy(() -> postService.searchPosts("Spring", cursor, null))
                .isInstanceOf(ClientException.class)
                .hasMessage("游标与搜索词不匹配");
    }

    @Test
    void searchPosts_whenCursorIsBlank_shouldTreatItAsFirstPage() {
        when(postMapper.searchPosts(eq("Java"), isNull(), eq(11))).thenReturn(List.of());

        PostSearchResultVO result = postService.searchPosts("Java", "   ", null);

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getNextCursor()).isNull();
        assertThat(result.getHasMore()).isFalse();
    }

    @Test
    void searchPosts_whenKeywordHasOuterWhitespace_shouldSearchWithTrimmedKeyword() {
        when(postMapper.searchPosts(eq("Java"), isNull(), eq(2))).thenReturn(List.of());

        PostSearchResultVO result = postService.searchPosts("  Java  ", null, 1);

        assertThat(result.getRecords()).isEmpty();
    }
}
