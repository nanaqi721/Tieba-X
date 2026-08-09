package com.mint.ai.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 点赞类型（对齐 post_like.target_type：1帖子 2评论）
 */
@AllArgsConstructor
public enum LikeTargetType {

    POST(1),
    COMMENT(2);

    @Getter
    private final Integer type;
}
