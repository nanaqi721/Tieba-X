package com.mint.ai.file.enums;

import com.mint.ai.common.enums.IErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文件上传错误码
 */
@Getter
@AllArgsConstructor
public enum FileErrorCode implements IErrorCode {

    FILE_NOT_NULL("A0501", "上传文件不能为空"),
    FILE_EXCEED_MAX_SIZE("A0502", "上传文件超出大小"),
    FILE_NOT_ALLOW_TYPE("A0503", "上传文件格式不正确"),
    FILE_EXCEED_MAX_NUM("A0504", "超出文件上传的数量");

    private final String code;

    private final String message;
}
