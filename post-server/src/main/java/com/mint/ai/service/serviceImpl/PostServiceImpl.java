package com.mint.ai.service.serviceImpl;

import com.mint.ai.common.vo.CreatePostVO;
import com.mint.ai.dto.CreatePostRequest;
import com.mint.ai.mapper.PostMapper;
import com.mint.ai.mapper.entiy.Post;
import com.mint.ai.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 帖子控制层
 */
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    @Override
    public CreatePostVO createPost(String barId, CreatePostRequest request) {

        Post post = Post.builder()
                .bar_id("1001")
                .user_id("1001")
                .title(request.getTitle())
                .content(request.getContent())
                .viewCount(0)
                .commentCount(0)
                .favoriteCount(0)
                .likeCount(0)
                .build();
        postMapper.insert(post);
        return CreatePostVO.builder()
                .id(post.getId())
                .build();

    }
}
