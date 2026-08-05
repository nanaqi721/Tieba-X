package com.mint.ai.service;

import com.mint.ai.common.vo.CreatePostVO;
import com.mint.ai.dto.CreatePostRequest;

/**
 * @param
 * @return
 */
public interface PostService {
    CreatePostVO createPost(String barId, CreatePostRequest request);
}
