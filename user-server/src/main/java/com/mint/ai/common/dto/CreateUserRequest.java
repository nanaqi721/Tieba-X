package com.mint.ai.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建用户请求实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度不符合要求")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度不符合要求")
    private String password;

    private String confirmPassword;

    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Min(value = 0, message = "性别取值需在0-2之间")
    @Max(value = 2, message = "性别取值需在0-2之间")
    private Integer sex;

    /**
     * 方法必须一is get has开头
     * ignore注解让在反序列化是忽略
     * @return
     */
    @AssertTrue(message = "两次输入的密码不一致")
    @JsonIgnore
    private boolean isPasswordConfirmed() {
        return password == null || confirmPassword == null || password.equals(confirmPassword);
    }
}
