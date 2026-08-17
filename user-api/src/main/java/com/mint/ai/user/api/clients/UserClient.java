package com.mint.ai.user.api.clients;

import com.mint.ai.common.Result;
import com.mint.ai.user.api.vo.UserBaseVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * user模块的 feign：供需要用户信息（昵称/头像）的其他服务调用
 */
@FeignClient(name = "user-server", path = "/api/users")
public interface UserClient {

    /**
     * 批量查询用户简要信息（昵称/头像），返回 userId -> UserBriefVO
     */
    @GetMapping("/v1/batch")
    Result<Map<String, UserBaseVO>> batchGetUsersByIds(@RequestParam("ids") List<String> ids);
}
