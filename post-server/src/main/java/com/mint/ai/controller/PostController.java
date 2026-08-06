package com.mint.ai.controller;

import com.mint.ai.common.Result;
import com.mint.ai.common.vo.CreatePostVO;
import com.mint.ai.dto.CreatePostRequest;
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
}
