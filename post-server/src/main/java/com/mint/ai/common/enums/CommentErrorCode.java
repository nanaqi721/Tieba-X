package com.mint.ai.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 评论模块错误码（业务模块内定义，实现 IErrorCode 接口）
 * <p>
 * 错误码格式：来源(A/B) + 模块号(03=评论) + 具体错误编号
 *   用户端：A03xx
 *   系统端：B03xx
 */
@Getter
@AllArgsConstructor
public enum CommentErrorCode implements IErrorCode {

    // ---- 用户端 A03xx ----
    COMMENT_NOT_FOUND("A0301", "评论不存在"),
    COMMENT_NO_PERMISSION("A0302", "无权删除该评论");

    private final String code;

    private final String message;
}
