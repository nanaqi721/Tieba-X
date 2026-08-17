package com.mint.ai.controller;

import com.mint.ai.common.Result;
import com.mint.ai.common.dto.CreateUserRequest;
import com.mint.ai.common.dto.LoginRequest;
import com.mint.ai.service.UserService;
import com.mint.ai.user.api.vo.UserBaseVO;
import com.mint.ai.utils.Results;
import com.mint.ai.util.DeviceTypeUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    /**
     * 批量查询用户简要信息（跨服务调用：帖子/楼层作者昵称头像），返回 userId -> UserBaeVo
     */
    @GetMapping("/v1/batch")
    public Result<Map<String, UserBaseVO>> batchGetUsersByIds(@RequestParam("ids") List<String> ids){
        return Results.success(userService.batchGetUsersByIds(ids));
    }

}
