package com.mint.ai.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.mint.ai.bar.api.dto.CreateBarRequest;
import com.mint.ai.bar.api.vo.BarDetailVO;
import com.mint.ai.common.Result;
import com.mint.ai.service.UserService;
import com.mint.ai.utils.Results;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 吧门面控制层（面向前端，经 Feign 调 bar-server）
 */
@RestController
@RequestMapping("/api/bars")
@RequiredArgsConstructor
public class BarController {

    private final UserService userService;

    /**
     * 创建吧
     */
    @SaCheckLogin
    @PostMapping("/v1/create")
    public Result<Long> createBar(@Valid @RequestBody CreateBarRequest request){
        return Results.success(userService.createBar(request));
    }

    /**
     * 查询吧详情（名称/图标/帖子数/关注数）
     */
    @SaCheckLogin
    @PostMapping("/v1/{barId}")
    public Result<BarDetailVO> queryBar(@PathVariable("barId") String barId){
        return Results.success(userService.queryBar(barId));
    }

    /**
     * 关注吧
     */
    @SaCheckLogin
    @PostMapping("/v1/{barId}/follow")
    public Result<Long> followBar(@PathVariable("barId") String barId){
        return Results.success(userService.followBar(barId));
    }

    /**
     * 取消关注吧
     */
    @SaCheckLogin
    @DeleteMapping("/v1/{barId}/unfollow")
    public Result<Long> unfollowBar(@PathVariable("barId") String barId){
        return Results.success(userService.unfollowBar(barId));
    }

    /**
     * 当前用户是否关注了该吧
     */
    @SaCheckLogin
    @GetMapping("/v1/{barId}/followed")
    public Result<Boolean> isFollowed(@PathVariable("barId") String barId){
        return Results.success(userService.isFollowed(barId));
    }
}
