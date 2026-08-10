package com.mint.ai.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * bar的错误码
 */
@Getter
@AllArgsConstructor
public enum BarErrorCode implements IErrorCode{

    BAR_NOT_FOUND("A0401","吧不存在"),
    BAR_NAME_EXISTS("A0402","吧名已存在");

    private final String code;

    private final String message;
}
