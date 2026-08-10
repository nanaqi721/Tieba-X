package com.mint.ai.service;

import com.mint.ai.post.api.dto.CreateCommentRequest;
import com.mint.ai.post.api.vo.CreateCommentVO;
import com.mint.ai.post.api.vo.FloorPageVO;

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

    /**
     * 楼层分页查询（页码分页）：本页顶层楼层 + 各自楼中楼子树
     */
    FloorPageVO listFloors(String postId, Integer pageNum, Integer pageSize);
}
