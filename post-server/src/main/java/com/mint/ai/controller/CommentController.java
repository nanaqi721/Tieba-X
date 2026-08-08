package com.mint.ai.controller;

import com.mint.ai.common.Result;
import com.mint.ai.utils.Results;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评论控制层
 */
@RestController
@RequestMapping("/api/posts")
public class CommentController {

    @PostMapping("/v1/{postId}/comments")
    public Result<Void> createComments(){

        return Results.success();
    }

}
