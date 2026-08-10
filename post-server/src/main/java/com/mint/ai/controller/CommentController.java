package com.mint.ai.controller;

import com.mint.ai.common.Result;
import com.mint.ai.post.api.dto.CreateCommentRequest;
import com.mint.ai.post.api.vo.CreateCommentVO;
import com.mint.ai.service.CommentLikeService;
import com.mint.ai.service.CommentService;
import com.mint.ai.utils.Results;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 评论控制层
 */
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    private final CommentLikeService commentLikeService;

    @PostMapping("/v1/{postId}/comments")
    public Result<CreateCommentVO> createComment(@PathVariable("postId") String postId, @Valid @RequestBody CreateCommentRequest request){
        return Results.success(commentService.createComment(postId, request));
    }

    @DeleteMapping("/v1/comments/{commentId}/delete")
    public Result<Void> commentDelete(@PathVariable("commentId") String commentId){
        commentService.deleteComment(commentId);
        return Results.success();
    }

    @PostMapping("/v1/comments/{commentId}/like")
    public Result<Long> commentLike(@PathVariable("commentId") String commentId){
        return Results.success(commentLikeService.like(commentId));
    }

    @DeleteMapping("/v1/comments/{commentId}/unlike")
    public Result<Long> commentUnlike(@PathVariable("commentId") String commentId){
        return Results.success(commentLikeService.unlike(commentId));
    }

    @GetMapping("/v1/comments/{commentId}/liked")
    public Result<Boolean> commentLiked(@PathVariable("commentId") String commentId){
        return Results.success(commentLikeService.liked(commentId));
    }

}
