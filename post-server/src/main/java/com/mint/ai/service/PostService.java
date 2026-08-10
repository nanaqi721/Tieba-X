package com.mint.ai.service;

import com.mint.ai.common.dto.UpdatePostRequest;
import com.mint.ai.post.api.dto.CreatePostRequest;
import com.mint.ai.post.api.vo.CreatePostVO;
import com.mint.ai.post.api.vo.PostDetailVO;
import com.mint.ai.post.api.vo.PostHomePageVO;
import com.mint.ai.post.api.vo.PostHomePageWithCursor;
import com.mint.ai.post.api.vo.PostSummaryVO;

import java.util.List;

/**
 * @param
 * @return
 */
public interface PostService {
    CreatePostVO createPost(String barId, CreatePostRequest request);

    void deletePostById(String barId, String postId);

    void updatePostById(String barId, UpdatePostRequest request);

    PostSummaryVO getPostSummary(String barId,String postId);

    /**
     * 帖子详情（content 全量不截断，不含楼层）
     */
    PostDetailVO getPostDetail(String postId);

    PostHomePageWithCursor postHomePage(String cursor, Integer pageSize);
}
