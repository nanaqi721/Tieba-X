package com.mint.ai.common;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 统一返回实体类
 */
@Data
@Accessors(chain = true)
public class Result<T> {

    public static final String SUCCESS_CODE = "200";

    /**
     * 状态码
     */
    private String code;

    /**
     * 数据
     */
    private T data;

    /**
     * 错误描述
     */
    private String message;
}
