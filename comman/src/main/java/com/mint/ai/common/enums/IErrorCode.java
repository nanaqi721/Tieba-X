package com.mint.ai.common.enums;

/**
 * 错误码接口
 * <p>
 * 所有错误码枚举统一实现该接口。异常体系（BizException、全局异常处理器）
 * 只依赖该接口获取 code/message，而不依赖具体模块的枚举，
 * 从而保持公共模块与业务模块解耦。
 */
public interface IErrorCode {

    /**
     * 错误码
     */
    String getCode();

    /**
     * 错误描述
     */
    String getMessage();
}
