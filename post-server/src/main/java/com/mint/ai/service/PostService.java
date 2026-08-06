package com.mint.ai.service;

import com.mint.ai.common.vo.CreatePostVO;
import com.mint.ai.dto.CreatePostRequest;
import com.mint.ai.dto.UpdatePostRequest;

/**
 * @param
 * @return
 */
public interface PostService {
    CreatePostVO createPost(String barId, CreatePostRequest request);

    void deletePostById(String barId, String postId);

    void updatePostById(String barId, UpdatePostRequest request);
}
