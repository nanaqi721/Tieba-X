package com.mint.ai.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.mint.ai.common.Result;
import com.mint.ai.post.api.dto.CreateCommentRequest;
import com.mint.ai.post.api.dto.CreatePostRequest;
import com.mint.ai.post.api.vo.CreateCommentVO;
import com.mint.ai.post.api.vo.CreatePostVO;
import com.mint.ai.post.api.vo.PostSummaryVO;
import com.mint.ai.service.UserService;
import com.mint.ai.utils.Results;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 帖子/评论门面控制层（面向前端，经 Feign 调 post-server）
 */
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final UserService userService;

    /**
     * 创建评论
     */
    @SaCheckLogin
    @PostMapping("/v1/{postId}/comments")
    public Result<CreateCommentVO> createComment(@PathVariable("postId") String postId, @Valid @RequestBody CreateCommentRequest request){
        return Results.success(userService.createComment(postId, request));
    }

    /**
     * 创建帖子
     */
    @SaCheckLogin
    @PostMapping("/v1/{barId}/create")
    public Result<CreatePostVO> createPost(@PathVariable("barId") String barId, @Valid @RequestBody CreatePostRequest request){
        return Results.success(userService.createPost(barId, request));
    }

    /**
     * 帖子摘要
     */
    @SaCheckLogin
    @GetMapping("/v1/{barId}")
    public Result<PostSummaryVO> getPostSummary(@PathVariable("barId") String barId, @RequestParam("postId") String postId){
        return Results.success(userService.getPostSummary(barId, postId));
    }
}
