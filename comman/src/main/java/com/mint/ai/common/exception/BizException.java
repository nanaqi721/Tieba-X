package com.mint.ai.common.exception;

import com.mint.ai.common.enums.IErrorCode;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 业务异常
 * <p>
 * 业务模块抛出时传入各自的错误码枚举（实现 IErrorCode），
 * 由全局异常处理器统一转换为 Result 返回。
 */
@Getter
public class BizException extends RuntimeException {

    private final String code;

    private final String message;

    public BizException(String message,Throwable throwable,IErrorCode errorCode) {
        super(message,throwable);
        this.code = errorCode.getCode();
        this.message = Optional.ofNullable(StringUtils.hasLength(message) ? message : null).orElse(errorCode.getMessage());
    }
}
