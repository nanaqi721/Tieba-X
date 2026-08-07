package com.mint.ai.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户模块错误码（业务模块内定义，实现 IErrorCode 接口）
 * <p>
 * 错误码格式：来源(A/B) + 模块号(01=用户) + 具体错误编号
 *   用户端：A01xx
 *   系统端：B01xx
 */
@Getter
@AllArgsConstructor
public enum UserErrorCode implements IErrorCode {

    // ---- 用户端 A01xx ----
    USER_NOT_FOUND("A0101", "用户不存在"),
    USER_BANNED("A0102", "用户已被禁用"),
    USERNAME_EMPTY("A0103", "用户名不能为空"),
    USERNAME_LENGTH_INVALID("A0104", "用户名长度需在4-20字符之间"),
    PASSWORD_EMPTY("A0105", "密码不能为空"),
    PASSWORD_LENGTH_INVALID("A0106", "密码长度需在6-20字符之间"),
    EMAIL_EMPTY("A0107", "邮箱不能为空"),
    EMAIL_FORMAT_INVALID("A0108", "邮箱格式不正确"),
    USERNAME_EXISTS("A0109", "用户名已被占用"),
    EMAIL_EXISTS("A0110", "邮箱已被注册"),
    LOGIN_ERROR("A0111", "用户名或密码错误"),
    USER_NO_PERMISSION("A0112", "无权限操作该用户"),

    // ---- 系统端 B01xx ----
    USER_SAVE_ERROR("B0101", "用户保存失败"),
    USER_UPDATE_ERROR("B0102", "用户更新失败"),
    USER_DELETE_ERROR("B0103", "用户删除失败"),
    USER_QUERY_ERROR("B0104", "用户查询失败"),
    PASSWORD_ENCRYPT_ERROR("B0105", "密码加密失败");

    private final String code;

    private final String message;
}
