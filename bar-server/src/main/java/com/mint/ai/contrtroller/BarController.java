package com.mint.ai.contrtroller;

import com.mint.ai.common.Result;
import com.mint.ai.bar.api.dto.CreateBarRequest;
import com.mint.ai.bar.api.vo.BarDetailVO;
import com.mint.ai.service.BarService;
import com.mint.ai.utils.Results;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 吧控制层
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bars")
public class BarController {


    private final BarService barService;

    /**
     * 创建吧 返回barId
     */
    @PostMapping("/v1/create")
    public Result<Long> createBar(@Valid @RequestBody CreateBarRequest request){
        return Results.success(barService.createBar(request));
    }

    /**
     * 查询吧的名称 图标 帖子数 关注数
     */
    @PostMapping("/v1/{barId}")
    public Result<BarDetailVO> queryBar(@PathVariable("barId") String barId){

        return Results.success(barService.queryBar(barId));
    }

    /**
     * 关注吧，返回最新粉丝数
     */
    @PostMapping("/v1/{barId}/follow")
    public Result<Long> followBar(@PathVariable("barId") String barId){
        return Results.success(barService.followBar(barId));
    }

    /**
     * 取消关注吧，返回最新粉丝数
     */
    @DeleteMapping("/v1/{barId}/unfollow")
    public Result<Long> unfollowBar(@PathVariable("barId") String barId){
        return Results.success(barService.unfollowBar(barId));
    }

    /**
     * 当前用户是否关注了该吧
     */
    @GetMapping("/v1/{barId}/followed")
    public Result<Boolean> isFollowed(@PathVariable("barId") String barId){
        return Results.success(barService.isFollowed(barId));
    }

    /**
     * 根据批量bar_id查询 吧名 吧缩略图 帖子数 关注数
     */
    @GetMapping("/v1/batch")
    public Result<Map<String, BarDetailVO>> queryBarList(@RequestParam("ids") List<String> ids) {
        return Results.success(barService.queryBarList(ids));
    }
}
