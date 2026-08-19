package com.mint.ai.mq.messgae;

import com.mint.ai.mq.enums.UserContentType;
import lombok.Data;

/**
 * 用户内容计数更新消息
 */
@Data
public class UserContentCountChangedMessage {

    private String userId;

    private String contentId;

    private UserContentType userContentType;
    // 内容增量
    private Integer delta;
}
