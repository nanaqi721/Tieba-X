package com.mint.ai.job;

import com.mint.ai.common.redisKey.RedisConstantKey;
import com.mint.ai.mapper.CommentMapper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 定时刷新评论点赞数
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CommentCountFlushJob {

    // 计数缓冲 hash 的业务模块名（与 CommentLikeServiceImpl 一致）
    private static final String METRIC = "comment_like_count";

    private final CommentMapper commentMapper;

    private final StringRedisTemplate stringRedisTemplate;

    @XxlJob("flushCommentLike")
    public void flushCommentLike() {
        String key = String.format(RedisConstantKey.COMMENT_COUNT_INCR, METRIC);
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        stringRedisTemplate.delete(key);
        if (entries.isEmpty()) {
            return;
        }

        List<CountIncr> flushList = entries.entrySet().stream()
                .map(e -> new CountIncr(String.valueOf(e.getKey()), Long.parseLong(String.valueOf(e.getValue()))))
                .collect(Collectors.toList());
        // 500 一批，单批失败丢弃不阻塞整体（同帖子任务）
        for (int i = 0; i < flushList.size(); i += 500) {
            try {
                commentMapper.batchIncrementCommentCount(
                        flushList.subList(i, Math.min(i + 500, flushList.size())));
            } catch (Exception e) {
                log.error("丢弃本次增量, size={}", Math.min(500, flushList.size() - i), e);
            }
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountIncr {

    private String id;

    private Long incrCount;
    }
}
