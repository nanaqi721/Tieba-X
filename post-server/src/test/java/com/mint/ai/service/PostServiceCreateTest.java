package com.mint.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mint.ai.bar.api.clients.BarClient;
import com.mint.ai.common.coontext.UserContext;
import com.mint.ai.common.dto.CreatePostRequest;
import com.mint.ai.mapper.AttachmentMapper;
import com.mint.ai.mapper.PostMapper;
import com.mint.ai.mapper.entity.AttachmentDO;
import com.mint.ai.mapper.entity.PostDO;
import com.mint.ai.mq.producer.UserContentChangedProducer;
import com.mint.ai.service.serviceImpl.PostServiceImpl;
import com.mint.ai.user.api.clients.UserClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceCreateTest {

    @Mock
    private PostMapper postMapper;

    @Mock
    private AttachmentMapper attachmentMapper;

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
                attachmentMapper,
                stringRedisTemplate,
                redissonClient,
                barClient,
                userClient,
                userContentChangedProducer
        );
        UserContext.setUserId("2001");
    }

    @AfterEach
    void tearDown() {
        UserContext.removeUserId();
    }

    @Test
    void createPost_whenImagesProvided_shouldPersistCoverAndOrderedAttachments() {
        when(postMapper.insert(any(PostDO.class))).thenAnswer(invocation -> {
            PostDO post = invocation.getArgument(0);
            post.setId("3001");
            return 1;
        });
        CreatePostRequest request = CreatePostRequest.builder()
                .barId("1001")
                .title("这是一个图片帖子")
                .content("帖子正文")
                .images(List.of("https://cdn.example/first.jpg", "https://cdn.example/second.png"))
                .build();

        postService.createPost(request);

        ArgumentCaptor<PostDO> postCaptor = ArgumentCaptor.forClass(PostDO.class);
        verify(postMapper).insert(postCaptor.capture());
        assertThat(postCaptor.getValue().getCoverImage()).isEqualTo("https://cdn.example/first.jpg");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AttachmentDO>> attachmentCaptor = ArgumentCaptor.forClass(List.class);
        verify(attachmentMapper).batchInsert(attachmentCaptor.capture());
        assertThat(attachmentCaptor.getValue())
                .extracting(AttachmentDO::getUrl)
                .containsExactly("https://cdn.example/first.jpg", "https://cdn.example/second.png");
        assertThat(attachmentCaptor.getValue())
                .extracting(AttachmentDO::getSortOrder)
                .containsExactly(0, 1);
        assertThat(attachmentCaptor.getValue())
                .allSatisfy(attachment -> {
                    assertThat(attachment.getId()).isNotBlank();
                    assertThat(attachment.getBizType()).isEqualTo(1);
                    assertThat(attachment.getBizId()).isEqualTo("3001");
                });
    }
}
