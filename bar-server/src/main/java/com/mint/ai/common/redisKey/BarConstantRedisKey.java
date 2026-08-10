package com.mint.ai.common.redisKey;

/**
 * 吧的redisKey常量
 */
public class BarConstantRedisKey {

    /**
     * 模块 业务 吧id
     */
    public static String BAR_DETAIL_CACHE = "bar_server:bar_cache:%s";

    public static String BAR_DETAIL_NULL_CACHE = "bar_server:bar_null_cache:%s";


    public static String BAR_UPDATE_DETAIL_CACHE_LOCK = "bar_server:update_cache_lock:%s";

    /**
     * 吧计数增量缓冲 key：bar_server:bar_count_incr:post_count，hash field=吧id
     */
    public static final String BAR_COUNT_INCR = "bar_server:bar_count_incr:%s";

    /**
     * 吧详情缓存 TTL（秒）
     */
    public static final String BAR_CACHE_TTL_SECONDS = "3600";
}
