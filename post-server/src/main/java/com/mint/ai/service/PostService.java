package com.mint.ai.service;

import com.mint.ai.common.dto.UpdatePostRequest;
import com.mint.ai.post.api.dto.CreatePostRequest;
import com.mint.ai.post.api.vo.CreatePostVO;
import com.mint.ai.post.api.vo.PostSummaryVO;

/**
 * @param
 * @return
 */
public interface PostService {
    CreatePostVO createPost(String barId, CreatePostRequest request);

    void deletePostById(String barId, String postId);

    void updatePostById(String barId, UpdatePostRequest request);

    PostSummaryVO getPostSummary(String barId,String postId);
}
