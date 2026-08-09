package com.mint.ai.job;

import com.mint.ai.common.redisKey.RedisKeyConstant;
import com.mint.ai.mapper.PostMapper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 帖子定时刷新数据库任务
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostCountFlushJob {

    private static final String[] METRICS = {"view_count","like_count","favorite_count","comment_count"};

    private final PostMapper postMapper;

    private final StringRedisTemplate stringRedisTemplate;


    @XxlJob("flushPostCount")
    public void flushPostCount() {
        for(String metric : METRICS) {
            String key = String.format(RedisKeyConstant.POST_COUNT_INCR, metric);
            // 获取和删除要强一致就加lua脚本但是我们是浏览量统计少一点没关系
            Map<Object, Object> incr = stringRedisTemplate.opsForHash().entries(key);
            stringRedisTemplate.delete(key);
            if(incr.isEmpty())
                continue;
            // 批量更新统计数据
            // 为什么不添加事务，因为如果第1次统计成功第二次失败第一次就没意义了
            List<PostCountFlushJob.CountIncr> list = incr.entrySet().stream()
                        .map(e -> {return new CountIncr((String) e.getKey(), Long.parseLong((String) e.getValue()));
                        })
                        .collect(Collectors.toList());
            for(int i=0;i<list.size();i+=500){
                try {
                    postMapper.batchIncrementPostCount(metric,
                            list.subList(i, Math.min(i + 500, list.size())));
                } catch (Exception e) {
                    log.error("丢弃本次增量, metric={}, size={}", metric, Math.min(500, list.size() - i), e);
                }
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
