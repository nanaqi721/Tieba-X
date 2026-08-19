package com.mint.ai.mq.event;

import com.mint.ai.mq.messgae.UserContentCountChangedMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户数据改变事件： 触发条件 创建删除帖子 评论删除评论
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserContentChangedEvent {

    private String eventId;

    private String eventMessage;

    private LocalDateTime createAt;

    private String topic;

    private String tag;

    private UserContentCountChangedMessage message;

}
