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
import com.mint.ai.common.vo.FeedPageVO;
import com.mint.ai.common.vo.FloorPageResponseVO;
import com.mint.ai.common.vo.PostDetailPageVO;
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

    /**
     * 主页帖子流（匿名）：帖子 + 所属吧详情
     */
    @GetMapping("/v1/feed")
    public Result<FeedPageVO> getFeed(@RequestParam(value = "cursor", required = false) String cursor,
                                      @RequestParam(value = "pageSize", required = false) Integer pageSize){
        return Results.success(userService.getFeed(cursor, pageSize));
    }

    /**
     * 帖子详情（匿名）：帖子 + 所属吧 + 作者
     */
    @GetMapping("/v1/detail")
    public Result<PostDetailPageVO> getPostDetail(@RequestParam("postId") String postId){
        return Results.success(userService.getPostDetail(postId));
    }

    /**
     * 楼层分页（匿名）：顶层楼层 + 楼中楼，作者昵称已填充
     */
    @GetMapping("/v1/floors")
    public Result<FloorPageResponseVO> getFloors(@RequestParam("postId") String postId,
                                                 @RequestParam(value = "pageNum", required = false) Integer pageNum,
                                                 @RequestParam(value = "pageSize", required = false) Integer pageSize){
        return Results.success(userService.getFloors(postId, pageNum, pageSize));
    }

    /**
     * 点赞帖子
     */
    @SaCheckLogin
    @PostMapping("/v1/{postId}/like")
    public Result<Long> postLike(@PathVariable("postId") String postId){
        return Results.success(userService.postLike(postId));
    }

    /**
     * 取消点赞帖子
     */
    @SaCheckLogin
    @DeleteMapping("/v1/{postId}/unlike")
    public Result<Long> postUnlike(@PathVariable("postId") String postId){
        return Results.success(userService.postUnlike(postId));
    }

    /**
     * 是否已点赞帖子
     */
    @SaCheckLogin
    @GetMapping("/v1/{postId}/liked")
    public Result<Boolean> postLiked(@PathVariable("postId") String postId){
        return Results.success(userService.postLiked(postId));
    }

    /**
     * 收藏帖子
     */
    @SaCheckLogin
    @PostMapping("/v1/{postId}/collect")
    public Result<Long> postCollect(@PathVariable("postId") String postId){
        return Results.success(userService.postCollect(postId));
    }

    /**
     * 取消收藏帖子
     */
    @SaCheckLogin
    @DeleteMapping("/v1/{postId}/uncollect")
    public Result<Long> postUncollect(@PathVariable("postId") String postId){
        return Results.success(userService.postUncollect(postId));
    }

    /**
     * 是否已收藏帖子
     */
    @SaCheckLogin
    @GetMapping("/v1/{postId}/collected")
    public Result<Boolean> postCollected(@PathVariable("postId") String postId){
        return Results.success(userService.postCollected(postId));
    }

    /**
     * 点赞评论
     */
    @SaCheckLogin
    @PostMapping("/v1/comments/{commentId}/like")
    public Result<Long> commentLike(@PathVariable("commentId") String commentId){
        return Results.success(userService.commentLike(commentId));
    }

    /**
     * 取消点赞评论
     */
    @SaCheckLogin
    @DeleteMapping("/v1/comments/{commentId}/unlike")
    public Result<Long> commentUnlike(@PathVariable("commentId") String commentId){
        return Results.success(userService.commentUnlike(commentId));
    }

    /**
     * 是否已点赞评论
     */
    @SaCheckLogin
    @GetMapping("/v1/comments/{commentId}/liked")
    public Result<Boolean> commentLiked(@PathVariable("commentId") String commentId){
        return Results.success(userService.commentLiked(commentId));
    }

    /**
     * 删除评论（作者或楼主，级联删楼中楼）
     */
    @SaCheckLogin
    @DeleteMapping("/v1/comments/{commentId}/delete")
    public Result<Void> commentDelete(@PathVariable("commentId") String commentId){
        userService.deleteComment(commentId);
        return Results.success();
    }


}
