package com.mint.ai.job;

import com.mint.ai.common.redisKey.BarConstantRedisKey;
import com.mint.ai.mapper.BarMapper;
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
 * 吧帖子数定时刷新数据库任务
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BarCountFlushJob {

    private static final int BATCH_SIZE = 500;

    private static final String[] METRICS = {"post_count", "follower_count"};

    private final BarMapper barMapper;

    private final StringRedisTemplate stringRedisTemplate;

    @XxlJob("flushBarPostCount")
    public void flushBarPostCount() {
        for(String metric : METRICS) {
            String key = String.format(BarConstantRedisKey.BAR_COUNT_INCR, metric);
            // 获取和删除要强一致就加lua脚本但是我们是计数统计少一点没关系
            Map<Object, Object> incr = stringRedisTemplate.opsForHash().entries(key);
            stringRedisTemplate.delete(key);
            if(incr.isEmpty())
                continue;
            // 批量更新统计数据
            // 为什么不添加事务，因为如果第1次统计成功第二次失败第一次就没意义了
            List<CountIncr> list = incr.entrySet().stream()
                    .map(e -> {return new CountIncr((String) e.getKey(), Long.parseLong((String) e.getValue()));
                    })
                    .collect(Collectors.toList());
            for(int i=0;i<list.size();i+=BATCH_SIZE){
                try {
                    barMapper.batchIncrementCount(metric,
                            list.subList(i, Math.min(i + BATCH_SIZE, list.size())));
                } catch (Exception e) {
                    log.error("丢弃本次增量, metric={}, size={}", metric, Math.min(BATCH_SIZE, list.size() - i), e);
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
