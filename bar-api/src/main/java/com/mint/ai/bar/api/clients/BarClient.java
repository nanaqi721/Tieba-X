package com.mint.ai.bar.api.clients;

import com.mint.ai.bar.api.dto.CreateBarRequest;
import com.mint.ai.bar.api.vo.BarDetailVO;
import com.mint.ai.common.Result;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * bar模块的 feign
 */
@FeignClient(name = "bar-server", path = "/api/bars")
public interface BarClient {

    /**
     * 创建吧，返回吧 id
     */
    @PostMapping("/v1/create")
    Result<Long> createBar(@Valid @RequestBody CreateBarRequest request);

    /**
     * 查询吧详情（名称 图标 帖子数 关注数）
     */
    @PostMapping("/v1/{barId}")
    Result<BarDetailVO> queryBar(@PathVariable("barId") String barId);

    /**
     * 批量查询吧详情（吧名 缩略图 帖子数 关注数），返回 barId -> BarDetailVO
     */
    @GetMapping("/v1/batch")
    Result<Map<String, BarDetailVO>> queryBarList(@RequestParam("ids") List<String> ids);

    /**
     * 关注吧，返回最新粉丝数
     */
    @PostMapping("/v1/{barId}/follow")
    Result<Long> followBar(@PathVariable("barId") String barId);

    /**
     * 取消关注吧，返回最新粉丝数
     */
    @DeleteMapping("/v1/{barId}/unfollow")
    Result<Long> unfollowBar(@PathVariable("barId") String barId);

    /**
     * 当前用户是否关注了该吧
     */
    @GetMapping("/v1/{barId}/followed")
    Result<Boolean> isFollowed(@PathVariable("barId") String barId);
}
