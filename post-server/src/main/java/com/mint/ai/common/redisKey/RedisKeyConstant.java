package com.mint.ai.common.redisKey;

/**
 * redis常量key
 */
public final class RedisKeyConstant {

    /**
     * 业务：帖子缓存：吧id：帖子id
     */
    public static final String POST_CACHE_SUMMARY = "post_server:post_cache:%s:%s";

    /**
     * 帖子摘要空缓存key
     */
    public static final String POST_NULL_CACHE_SUMMARY = "post_server:post_null_cache:%s:%s";

    public static final String POST_SUMMARY_LOCK = "post_server:lock:post_summary:%s:%s";

    /**
     * 模块：业务：点赞 or 收藏 or 浏览量
     */
    public static final String POST_COUNT_INCR = "post_server:post_count_incr:%s";

    public static final String COMMENT_COUNT_INCR = "comment_server:comment_count_incr:%s";

    /**
     * 吧计数增量缓冲 key（bar-server 定时任务消费）：bar_server:bar_count_incr:post_count，hash field=吧id
     * 注意：与 bar-server BarConstantRedisKey.BAR_COUNT_INCR 同值，跨服务靠命名约定对齐
     */
    public static final String BAR_COUNT_INCR = "bar_server:bar_count_incr:%s";
}
