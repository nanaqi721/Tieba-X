package com.mint.ai.service;

import com.mint.ai.common.dto.CreateUserRequest;
import com.mint.ai.common.dto.LoginRequest;

/**
 * 用户模块服务层
 */
public interface UserService {
    String register(CreateUserRequest request, String deviceType);

    /**
     * 用户登录
     * @param request 登录请求体
     * @param deviceType 终端类型（pc/mobile/tablet）
     * @return token
     */
    String login(LoginRequest request, String deviceType);

    /**
     * 用户登出
     */
    void logout();
}
