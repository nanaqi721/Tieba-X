package com.mint.ai.controller;

import com.mint.ai.common.Result;
import com.mint.ai.common.dto.CreateUserRequest;
import com.mint.ai.common.dto.LoginRequest;
import com.mint.ai.service.UserService;
import com.mint.ai.utils.Results;
import com.mint.ai.util.DeviceTypeUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户模块控制层
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 用户注册接口
     * @param request 用户注册请求体
     * @return 用户id
     */
    @PostMapping("/v1/register")
    public Result<String> register(@Valid @RequestBody CreateUserRequest request, HttpServletRequest httpRequest){
        return Results.success(userService.register(request, DeviceTypeUtil.detect(httpRequest)));
    }

    /**
     * 用户登录接口
     * @param request 登录请求体
     * @return token
     */
    @PostMapping("/v1/login")
    public Result<String> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest){
        return Results.success(userService.login(request, DeviceTypeUtil.detect(httpRequest)));
    }

    /**
     * 用户登出接口
     * @return 空
     */
    @PostMapping("/v1/logout")
    public Result<Void> logout(){
        userService.logout();
        return Results.success();
    }
}
