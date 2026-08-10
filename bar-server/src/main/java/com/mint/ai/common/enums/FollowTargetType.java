package com.mint.ai.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 关注目标类型（对齐 follow.target_type：1吧 2用户）
 */
@AllArgsConstructor
public enum FollowTargetType {

    BAR(1),
    USER(2);

    @Getter
    private final Integer type;
}
