package com.mint.ai.controller;

import com.mint.ai.common.Result;
import com.mint.ai.common.dto.CreateCommentRequest;
import com.mint.ai.common.vo.CreateCommentVO;
import com.mint.ai.common.vo.FloorPageResponseVO;
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
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    private final CommentLikeService commentLikeService;

    /**
     * 创建评论
     * @param postId 帖子id
     * @param request
     * @return
     */
    @PostMapping("/v1/{postId}/comments")
    public Result<CreateCommentVO> createComment(@PathVariable("postId") String postId, @Valid @RequestBody CreateCommentRequest request){
        return Results.success(commentService.createComment(postId, request));
    }

    /**
     * 分页查询帖子评论
     * @param postId 帖子id
     * @param pageNum 分页页码
     * @param pageSize 分页数
     * @return
     */
    @GetMapping("/v1/page")
    public Result<FloorPageResponseVO> getComments(@RequestParam("postId") String postId,
                                                 @RequestParam(value = "pageNum", required = false) Integer pageNum,
                                                 @RequestParam(value = "pageSize", required = false) Integer pageSize){
        return Results.success(commentService.pageQueryComments(postId, pageNum, pageSize));
    }

    /**
     * 查询某一楼层下子评论
     * @param rootId 本楼层id，所有该评论下的子评论都会有这个属性，通过它来简化查询
     * @param postId 帖子id
     * @param pageNum 分页号码
     * @param pageSize 分页大小
     * @return
     */
    @GetMapping("/v1/replies/page")
    public Result<FloorPageResponseVO> getReplies( @RequestParam("rootId") String rootId,
                                                   @RequestParam("postId") String postId,
                                                   @RequestParam(value = "pageNum", required = false) Integer pageNum,
                                                   @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return Results.success(commentService.pageQueryReplies(postId, rootId, pageNum, pageSize));
    }

    /**
     * 根据评论id删除评论
     * @param commentId 评论id
     * @return
     */
    @DeleteMapping("/v1/comments/{commentId}/delete")
    public Result<Void> commentDelete(@PathVariable("commentId") String commentId){
        commentService.deleteComment(commentId);
        return Results.success();
    }

    /**
     * 点赞评论
     * @param commentId 评论id
     * @return
     */
    @PostMapping("/v1/comments/{commentId}/like")
    public Result<Long> commentLike(@PathVariable("commentId") String commentId){
        return Results.success(commentLikeService.like(commentId));
    }

    /**
     * 取消点赞
     * @param commentId 评论id
     * @return
     */
    @DeleteMapping("/v1/comments/{commentId}/unlike")
    public Result<Long> commentUnlike(@PathVariable("commentId") String commentId){
        return Results.success(commentLikeService.unlike(commentId));
    }

    /**
     * 是否点赞评论
     * @param commentId 评论id
     * @return
     */
    @GetMapping("/v1/comments/{commentId}/liked")
    public Result<Boolean> commentLiked(@PathVariable("commentId") String commentId){
        return Results.success(commentLikeService.liked(commentId));
    }

}
