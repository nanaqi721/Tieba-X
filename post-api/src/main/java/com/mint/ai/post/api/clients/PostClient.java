package com.mint.ai.post.api.clients;

import com.mint.ai.common.Result;
import com.mint.ai.post.api.dto.CreateCommentRequest;
import com.mint.ai.post.api.dto.CreatePostRequest;
import com.mint.ai.post.api.vo.CreateCommentVO;
import com.mint.ai.post.api.vo.CreatePostVO;
import com.mint.ai.post.api.vo.PostSummaryVO;
import org.springframework.cloud.openfeign.FeignClient;
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

    @PostMapping("/v1/{postId}/comments")
    public Result<CreateCommentVO> createComment(@PathVariable("postId") String postId, @RequestBody CreateCommentRequest request);

    @PostMapping("/v1/{barId}/create")
    public Result<CreatePostVO> createPost(@PathVariable("barId") String barId, @RequestBody CreatePostRequest request);
}
