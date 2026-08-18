package com.mint.ai.controller;

import com.mint.ai.common.Result;
import com.mint.ai.common.dto.PostFeedInBarRequest;
import com.mint.ai.common.dto.UpdatePostRequest;
import com.mint.ai.common.vo.PostFeedInBarVO;
import com.mint.ai.post.api.dto.CreatePostRequest;
import com.mint.ai.post.api.vo.*;
import com.mint.ai.service.CommentService;
import com.mint.ai.service.PostFavoriteService;
import com.mint.ai.service.PostLikeService;
import com.mint.ai.service.PostService;
import com.mint.ai.utils.Results;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 帖子控制层
 */
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    private final PostLikeService postLikeService;

    private final PostFavoriteService postFavoriteService;

    private final CommentService commentService;

    @PostMapping("/v1/{barId}/create")
    public Result<PostCreateVO> createPost(@PathVariable(value = "barId") String barId, @Valid @RequestBody CreatePostRequest request){
        PostCreateVO post = postService.createPost(barId, request);
        return Results.success(post);
    }

    @DeleteMapping("/v1/{barId}/delete")
    public Result<Void> deletePostById(@PathVariable(value = "barId") String barId,@RequestParam(value = "postId") String postId){
        postService.deletePostById(barId,postId);
        return Results.success();
    }

    @PutMapping("/v1/{barId}/delete")
    public Result<Void> updatePostById(@PathVariable(value = "barId") String barId, @RequestBody UpdatePostRequest request){
        postService.updatePostById(barId,request);
        return Results.success();
    }

    @PostMapping("/v1/{barId}/home")
    public Result<PostFeedInBarVO> postFeedInBar(@PathVariable(value = "barId") String barId, @RequestBody PostFeedInBarRequest request){
        return Results.success(postService.getPostFeedInBar(barId,request));
    }

    @GetMapping("/v1/{barId}")
    public Result<PostSummaryVO> getPostSummary(@PathVariable("barId") String barId,@RequestParam("postId") String request){
        PostSummaryVO postSummaryVO =postService.getPostSummary(barId,request);
        return Results.success(postSummaryVO);
    }

    @GetMapping("/v1/floors")
    public Result<FloorPageResponseVO> getFloors(@RequestParam("postId") String postId,
                                                 @RequestParam(value = "pageNum", required = false) Integer pageNum,
                                                 @RequestParam(value = "pageSize", required = false) Integer pageSize){
        return Results.success(commentService.listFloors(postId, pageNum, pageSize));
    }

    @GetMapping("/v1/floors/{rootId}/replies")
    public Result<FloorPageResponseVO> getReplies(@PathVariable("rootId") String rootId,
                                                   @RequestParam("postId") String postId,
                                                   @RequestParam(value = "pageNum", required = false) Integer pageNum,
                                                   @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return Results.success(commentService.listReplies(postId, rootId, pageNum, pageSize));
    }

    @GetMapping("/v1/floors/{floorId}/thread")
    public Result<List<FloorVO>> getFloorThread(@PathVariable("floorId") String floorId,
                                                 @RequestParam("postId") String postId) {
        return Results.success(commentService.listFloorThread(postId, floorId));
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

    @PostMapping("/v1/{postId}/collect")
    public Result<Long> postCollect(@PathVariable("postId") String postId){
        return Results.success(postFavoriteService.postCollect(postId));
    }

    @DeleteMapping("/v1/{postId}/uncollect")
    public Result<Long> postUncollect(@PathVariable("postId") String postId){
        return Results.success(postFavoriteService.postUncollect(postId));
    }

    @GetMapping("/v1/{postId}/collected")
    public Result<Boolean> postCollected(@PathVariable("postId") String postId){
        return Results.success(postFavoriteService.postCollected(postId));
    }

    /**
     * 主页帖子流：按热度游标查询，聚合吧信息
     */
    @GetMapping("/v1/feed")
    public Result<PostFeedItemVO> getFeed(@RequestParam(value = "cursor", required = false) String cursor,
                                          @RequestParam(value = "pageSize", required = false) Integer pageSize){
        return Results.success(postService.getFeed(cursor, pageSize));
    }


}
