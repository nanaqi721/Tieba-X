package com.mint.ai.controller;

import com.mint.ai.common.Result;
import com.mint.ai.common.dto.UpdatePostRequest;
import com.mint.ai.post.api.dto.CreatePostRequest;
import com.mint.ai.post.api.vo.CreatePostVO;
import com.mint.ai.post.api.vo.PostSummaryVO;
import com.mint.ai.service.PostLikeService;
import com.mint.ai.service.PostService;
import com.mint.ai.utils.Results;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 帖子控制层
 */
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    private final PostLikeService postLikeService;

    @PostMapping("/v1/{barId}/create")
    public Result<CreatePostVO> createPost(@PathVariable(value = "barId") String barId, @Valid @RequestBody CreatePostRequest request){
        CreatePostVO post = postService.createPost(barId, request);
        return Results.success(post);
    }

    @DeleteMapping("/v1/{bardId}/delete")
    public Result<Void> deletePostById(@PathVariable(value = "barId") String barId,@RequestParam(value = "postId") String postId){
        postService.deletePostById(barId,postId);
        return Results.success();
    }

    @PutMapping("/v1/{bardId}/delete")
    public Result<Void> updatePostById(@PathVariable(value = "barId") String barId, @RequestBody UpdatePostRequest request){
        postService.updatePostById(barId,request);
        return Results.success();
    }

    @GetMapping("/v1/{barId}")
    public Result<PostSummaryVO> getPostSummary(@PathVariable("barId") String barId,@RequestParam("postId") String postId){
        PostSummaryVO postSummaryVO =postService.getPostSummary(barId,postId);
        return Results.success(postSummaryVO);
    }

    @PostMapping("/v1/{postId}/like")
    public Result<Long> postLike(@PathVariable("postId") String postId){
        return Results.success(postLikeService.postLike(postId));
    }

    @DeleteMapping("/v1/{postId}/unlike")
    public Result<Long> postUnlike(@PathVariable("postId") String postId){
        return Results.success(postLikeService.postUnlike(postId));
    }

    @GetMapping("/v1/{postId}/liked")
    public Result<Boolean> postLiked(@PathVariable("postId") String postId){

        return Results.success(postLikeService.postLiked(postId));
    }
}
