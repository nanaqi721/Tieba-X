package com.mint.ai.mq.constant;

/**
 * 用户内容计数消息的公共契约。
 */
public final class UserContentMqConstants {

    public static final String TOPIC = "user-content-changed";

    public static final String CONSUMER_GROUP = "user-content-count-consumer";

    private UserContentMqConstants() {
    }
}
