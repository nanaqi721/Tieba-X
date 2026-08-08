package com.mint.ai.service;

import com.mint.ai.post.api.dto.CreateCommentRequest;
import com.mint.ai.post.api.vo.CreateCommentVO;

/**
 * 评论服务层
 */
public interface CommentService {
    CreateCommentVO createComment(String postId, CreateCommentRequest request);
}
