package com.mint.ai.service;

import com.mint.ai.common.dto.CreateUserRequest;
import com.mint.ai.common.dto.LoginRequest;
import com.mint.ai.user.api.vo.UserBaseVO;

import java.util.List;
import java.util.Map;

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

    /**
     * 批量查询用户简要信息（昵称/头像），供 post-server 等跨服务展示作者时调用
     * @param ids 用户 id 列表
     * @return userId -> UserBriefVO
     */
    Map<String, UserBaseVO> batchGetUsersByIds(List<String> ids);
}
