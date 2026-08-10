package com.mint.ai.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * bar的错误码
 */
@Getter
@AllArgsConstructor
public enum BarErrorCode {

    BAR_NOT_FOUND("A0401","帖子不存在");

    private final String code;

    private final String message;
}
