package com.mint.ai.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用错误码枚举（公共模块，与具体业务无关）
 * <p>
 * 错误码为 String，共 5 位：
 *   第 1 位   ：错误来源（A=用户端错误  B=系统端错误  C=第三方服务错误）
 *   第 2-3 位 ：模块号（00 通用）
 *   第 4-5 位 ：具体错误编号
 * 正常返回固定五个零：00000
 * <p>
 * 业务模块的错误码请在各模块内定义并实现 IErrorCode 接口，
 * 不要放入公共模块，避免公共模块被业务错误码污染。
 */
@Getter
@AllArgsConstructor
public enum BaseEnums implements IErrorCode {

    SUCCESS("00000", "成功"),
    USER_ERROR("A0001", "用户端错误"),
    SYSTEM_ERROR("B0001", "系统执行出错"),
    THIRD_PARTY_ERROR("C0001", "调用第三方服务出错");

    private final String code;

    private final String message;
}
