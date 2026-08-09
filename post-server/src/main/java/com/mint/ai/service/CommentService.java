package com.mint.ai.service;

import com.mint.ai.post.api.dto.CreateCommentRequest;
import com.mint.ai.post.api.vo.CreateCommentVO;

/**
 * 评论服务层
 */
public interface CommentService {
    CreateCommentVO createComment(String postId, CreateCommentRequest request);

    /**
     * 删除评论（作者或楼主可删，级联删除楼中楼）
     * @param commentId 评论 id
     */
    void deleteComment(String commentId);
}
