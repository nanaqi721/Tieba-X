package com.mint.ai.service;

/**
 * 评论点赞业务层
 */
public interface CommentLikeService {

    Long like(String commentId);

    Long unlike(String commentId);

    Boolean liked(String commentId);
}
