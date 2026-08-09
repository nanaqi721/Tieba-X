package com.mint.ai.service;

/**
 * 帖子点赞业务层
 */
public interface PostLikeService {
    Long postLike(String postId);

    Long postUnlike(String postId);

    Boolean postLiked(String postId);
}
