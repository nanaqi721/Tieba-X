package com.mint.ai.controller;

import com.mint.ai.common.Result;
import com.mint.ai.common.dto.PostFeedInBarRequest;
import com.mint.ai.common.dto.UpdatePostRequest;
import com.mint.ai.common.vo.PostFeedInBarVO;
import com.mint.ai.common.dto.CreatePostRequest;
import com.mint.ai.common.vo.*;
import com.mint.ai.service.PostFavoriteService;
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

    private final PostFavoriteService postFavoriteService;

    /**
     * 创建帖子
     */
    @PostMapping("/v1/create")
    public Result<PostCreateVO> createPost(@Valid @RequestBody CreatePostRequest request){
        PostCreateVO post = postService.createPost(request);
        return Results.success(post);
    }

    /**
     * 删除帖子
     */
    @DeleteMapping("/v1/delete")
    public Result<Void> deletePostById(@RequestParam(value = "postId") String postId){
        postService.deletePostById(postId);
        return Results.success();
    }

    /**
     * 更新帖子
     * @param request 更新帖子请求体
     * @return
     */
    @PutMapping("/v1/delete")
    public Result<Void> updatePostById(@RequestBody UpdatePostRequest request){
        postService.updatePostById(request);
        return Results.success();
    }

    /**
     * 在bar主页帖子流式查询
     * @param request 流式查询请求体
     * @return
     */
    @PostMapping("/v1/bars/home/posts")
    public Result<PostFeedInBarVO> postFeedInBar(@RequestBody PostFeedInBarRequest request){
        return Results.success(postService.getPostFeedInBar(request));
    }

    /**
     * 获取帖子的详细信息
     * @param request 帖子id
     * @return
     */
    @GetMapping("/v1/query")
    public Result<PostSummaryVO> getPostSummary(@RequestParam("postId") String request){
        PostSummaryVO postSummaryVO =postService.getPostSummary(request);
        return Results.success(postSummaryVO);
    }

    @GetMapping("/v1/search")
    public Result<PostSearchResultVO> searchPosts(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return Results.success(postService.searchPosts(keyword, cursor, pageSize));
    }

    /**
     * 点赞帖子
     * @param postId
     * @return
     */
    @PostMapping("/v1/{postId}/like")
    public Result<Long> postLike(@PathVariable("postId") String postId){
        return Results.success(postLikeService.postLike(postId));
    }

    /**
     * 取消点赞
     * @param postId
     * @return
     */
    @DeleteMapping("/v1/{postId}/unlike")
    public Result<Long> postUnlike(@PathVariable("postId") String postId){
        return Results.success(postLikeService.postUnlike(postId));
    }

    /**
     * 是否点赞
     * @param postId
     * @return
     */
    @GetMapping("/v1/{postId}/liked")
    public Result<Boolean> postLiked(@PathVariable("postId") String postId){

        return Results.success(postLikeService.postLiked(postId));
    }

    /**
     * 收藏帖子
     * @param postId
     * @return
     */
    @PostMapping("/v1/{postId}/collect")
    public Result<Long> postCollect(@PathVariable("postId") String postId){
        return Results.success(postFavoriteService.postCollect(postId));
    }

    /**
     * 取消收藏
     * @param postId
     * @return
     */
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
    @GetMapping("/v1/home/feed")
    public Result<PostFeedItemVO> HomePostsFeed(@RequestParam(value = "cursor", required = false) String cursor,
                                          @RequestParam(value = "pageSize", required = false) Integer pageSize){
        return Results.success(postService.homePostsFeed(cursor, pageSize));
    }


}
