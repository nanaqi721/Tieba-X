package com.mint.ai.service;

/**
 * 帖子收藏服务层
 */
public interface PostFavoriteService {

    Long postCollect(String postId);

    Long postUncollect(String postId);

    Boolean postCollected(String postId);
}
