package com.mint.ai.mq.producer;

import com.alibaba.fastjson.JSON;
import com.mint.ai.mq.constant.UserContentMqConstants;
import com.mint.ai.mq.enums.UserContentType;
import com.mint.ai.mq.event.UserContentChangedEvent;
import com.mint.ai.mq.messgae.UserContentCountChangedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户评论/回复数量生产者
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserContentChangedProducer {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 在当前数据库事务提交后发送消息；没有事务时立即发送。
     */
    public void send(UserContentChangedEvent event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        doSend(event);
                    } catch (RuntimeException e) {
                        // 数据库此时已经提交，不能再把异常抛给接口调用方造成“业务失败”的假象。
                        log.error("数据库事务已提交，但 RocketMQ 消息最终发送失败: eventId={}",
                                event.getEventId());
                    }
                }
            });
            return;
        }
        doSend(event);
    }

    public void send(String userId, UserContentType type, Integer delta,
                     String eventMessage, String contentId) {
        send(buildEvent(userId, type, delta, eventMessage, contentId));
    }

    private SendResult doSend(UserContentChangedEvent event) {
        Message<UserContentCountChangedMessage> message = MessageBuilder.withPayload(event.getMessage())
                .setHeader(RocketMQHeaders.KEYS, event.getEventId())
                .build();
        try {
            SendResult sendResult = rocketMQTemplate.syncSend(
                    event.getTopic(),
                    message);
            log.info("RocketMQ 消息发送成功: eventId={}, messageId={}",
                    event.getEventId(), sendResult.getMsgId());
            return sendResult;
        } catch (Exception e) {
            log.error("RocketMQ 消息发送失败: eventId={}, payload={}",
                    event.getEventId(), JSON.toJSONString(event.getMessage()), e);
            throw e;
        }
    }

    public UserContentChangedEvent buildEvent(String userId, UserContentType type, Integer delta,
                                              String eventMessage, String contentId) {

        UserContentCountChangedMessage userContentCountChangedMessage = new UserContentCountChangedMessage();
        userContentCountChangedMessage.setUserId(userId);
        userContentCountChangedMessage.setContentId(contentId);
        userContentCountChangedMessage.setUserContentType(type);
        userContentCountChangedMessage.setDelta(delta);

        return UserContentChangedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventMessage(eventMessage)
                .topic(UserContentMqConstants.TOPIC)
                .message(userContentCountChangedMessage)
                .createAt(LocalDateTime.now())
                .build();

    }
}
