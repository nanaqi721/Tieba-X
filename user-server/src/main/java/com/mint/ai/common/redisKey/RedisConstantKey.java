package com.mint.ai.common.redisKey;

/**
 * redis的key
 */
public final class RedisConstantKey {

    /**
     * 业务 用户缓存 用户id
     */
    public static final  String USER_CACHE = "user_server:user_cache:%s";

    public static final String USER_NULL_CACHE = "user_server:user_null_cache:%s";

    public static final String USER_CACHE_LOCK = "user_server:user_cache_lock:%s";
}
