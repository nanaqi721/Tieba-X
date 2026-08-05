package com.mint.ai.common.redisKey;

/**
 * redis常量key
 */
public final class RedisKeyConstant {

    /**
     * 业务：帖子缓存：吧名：帖子id
     */
    public static final String POST_CACHE_DETAIL = "post_server:post_cache:%s:%s";

    /**
     * 模块：业务：帖子id
     */
    public static final String POST_COUNT_INCR = "post_server:post_count_incr:%s";
}
