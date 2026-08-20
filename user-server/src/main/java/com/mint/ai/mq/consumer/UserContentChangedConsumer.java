package com.mint.ai.mq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mint.ai.mq.constant.UserContentMqConstants;
import com.mint.ai.mq.messgae.UserContentCountChangedMessage;
import com.mint.ai.mq.service.UserContentCountConsumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@RocketMQMessageListener(
        topic = UserContentMqConstants.TOPIC,
        consumerGroup = UserContentMqConstants.CONSUMER_GROUP
)
public class UserContentChangedConsumer implements RocketMQListener<MessageExt> {

    private final ObjectMapper objectMapper;

    private final UserContentCountConsumeService consumeService;

    @Override
    public void onMessage(MessageExt messageExt) {
        String eventId = messageExt.getKeys();
        try {
            UserContentCountChangedMessage message = objectMapper.readValue(
                    messageExt.getBody(), UserContentCountChangedMessage.class);
            consumeService.consume(eventId, message);
            log.info("RocketMQ 消息消费成功: eventId={}, messageId={}",
                    eventId, messageExt.getMsgId());
        } catch (Exception e) {
            log.error("RocketMQ 消息消费失败: eventId={}, messageId={}",
                    eventId, messageExt.getMsgId(), e);
            throw new IllegalStateException("消费用户内容计数消息失败", e);
        }
    }
}
