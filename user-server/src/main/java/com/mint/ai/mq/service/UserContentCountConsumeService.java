package com.mint.ai.mq.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.mint.ai.common.redisKey.RedisConstantKey;
import com.mint.ai.mapper.MqConsumeRecordMapper;
import com.mint.ai.mapper.UserMapper;
import com.mint.ai.mq.constant.UserContentMqConstants;
import com.mint.ai.mq.messgae.UserContentCountChangedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserContentCountConsumeService {

    private final MqConsumeRecordMapper mqConsumeRecordMapper;

    private final UserMapper userMapper;

    private final StringRedisTemplate stringRedisTemplate;

    @Transactional
    public void consume(String eventId, UserContentCountChangedMessage message) {
        validate(eventId, message);

        int inserted = mqConsumeRecordMapper.insertIgnore(
                IdWorker.getIdStr(),
                UserContentMqConstants.CONSUMER_GROUP,
                eventId,
                UserContentMqConstants.TOPIC);
        if (inserted == 0) {
            log.info("忽略重复 RocketMQ 消息: eventId={}", eventId);
            return;
        }

        int affected = switch (message.getUserContentType()) {
            case POST -> userMapper.incrementPostCount(message.getUserId(), message.getDelta());
            case COMMENT -> userMapper.incrementCommentCount(message.getUserId(), message.getDelta());
        };
        if (affected != 1) {
            throw new IllegalStateException("用户计数更新失败, userId=" + message.getUserId());
        }
        // 更新数据库删除缓存 cache-aside
        stringRedisTemplate.delete(String.format(RedisConstantKey.USER_CACHE,message.getUserId()));
    }

    private void validate(String eventId, UserContentCountChangedMessage message) {
        if (!StringUtils.hasText(eventId)) {
            throw new IllegalArgumentException("RocketMQ 消息缺少 eventId header");
        }
        if (message == null
                || !StringUtils.hasText(message.getUserId())
                || !StringUtils.hasText(message.getContentId())
                || message.getUserContentType() == null
                || message.getDelta() == null
                || message.getDelta() == 0) {
            throw new IllegalArgumentException("RocketMQ 用户计数消息不完整");
        }
    }
}
