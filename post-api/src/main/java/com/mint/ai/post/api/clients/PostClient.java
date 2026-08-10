package com.mint.ai.post.api.clients;

import com.mint.ai.common.Result;
import com.mint.ai.post.api.dto.CreateCommentRequest;
import com.mint.ai.post.api.dto.CreatePostRequest;
import com.mint.ai.post.api.vo.CreateCommentVO;
import com.mint.ai.post.api.vo.CreatePostVO;
import com.mint.ai.post.api.vo.FloorPageVO;
import com.mint.ai.post.api.vo.PostDetailVO;
import com.mint.ai.post.api.vo.PostHomePageWithCursor;
import com.mint.ai.post.api.vo.PostSummaryVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * post模块的fegin
 */
@FeignClient(name = "post-server",path = "/api/posts")
public interface PostClient {

    @GetMapping("/v1/{barId}")
    public Result<PostSummaryVO> getPostSummary(@PathVariable("barId") String barId, @RequestParam("postId") String postId);

    /**
     * 帖子详情（content 全量，不含楼层）
     */
    @GetMapping("/v1/detail")
    public Result<PostDetailVO> getPostDetail(@RequestParam("postId") String postId);

    /**
     * 楼层分页（页码分页，顶层楼层+楼中楼）
     */
    @GetMapping("/v1/floors")
    public Result<FloorPageVO> listFloors(@RequestParam("postId") String postId,
                                          @RequestParam(value = "pageNum", required = false) Integer pageNum,
                                          @RequestParam(value = "pageSize", required = false) Integer pageSize);

    /**
     * 主页帖子按热度游标分页（滑动查询）
     */
    @GetMapping("/v1/home/feed")
    public Result<PostHomePageWithCursor> postHomePage(@RequestParam(value = "cursor", required = false) String cursor,
                                                       @RequestParam(value = "pageSize", required = false) Integer pageSize);

    @PostMapping("/v1/{postId}/comments")
    public Result<CreateCommentVO> createComment(@PathVariable("postId") String postId, @RequestBody CreateCommentRequest request);

    @PostMapping("/v1/{barId}/create")
    public Result<CreatePostVO> createPost(@PathVariable("barId") String barId, @RequestBody CreatePostRequest request);

    @PostMapping("/v1/{postId}/like")
    public Result<Long> postLike(@PathVariable("postId") String postId);

    @DeleteMapping("/v1/{postId}/unlike")
    public Result<Long> postUnlike(@PathVariable("postId") String postId);

    @GetMapping("/v1/{postId}/liked")
    public Result<Boolean> postLiked(@PathVariable("postId") String postId);

    @PostMapping("/v1/{postId}/collect")
    public Result<Long> postCollect(@PathVariable("postId") String postId);

    @DeleteMapping("/v1/{postId}/uncollect")
    public Result<Long> postUncollect(@PathVariable("postId") String postId);

    @GetMapping("/v1/{postId}/collected")
    public Result<Boolean> postCollected(@PathVariable("postId") String postId);

    @PostMapping("/v1/comments/{commentId}/like")
    public Result<Long> commentLike(@PathVariable("commentId") String commentId);

    @DeleteMapping("/v1/comments/{commentId}/unlike")
    public Result<Long> commentUnlike(@PathVariable("commentId") String commentId);

    @GetMapping("/v1/comments/{commentId}/liked")
    public Result<Boolean> commentLiked(@PathVariable("commentId") String commentId);

    @DeleteMapping("/v1/comments/{commentId}/delete")
    public Result<Void> commentDelete(@PathVariable("commentId") String commentId);
}
