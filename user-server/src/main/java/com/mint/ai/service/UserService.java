package com.mint.ai.service;

import com.mint.ai.common.dto.CreateUserRequest;
import com.mint.ai.common.dto.LoginRequest;
import com.mint.ai.post.api.dto.CreateCommentRequest;
import com.mint.ai.post.api.dto.CreatePostRequest;
import com.mint.ai.post.api.vo.CreateCommentVO;
import com.mint.ai.post.api.vo.CreatePostVO;
import com.mint.ai.post.api.vo.PostSummaryVO;

/**
 * 用户模块服务层
 */
public interface UserService {
    String register(CreateUserRequest request, String deviceType);

    /**
     * 用户登录
     * @param request 登录请求体
     * @param deviceType 终端类型（pc/mobile/tablet）
     * @return token
     */
    String login(LoginRequest request, String deviceType);

    /**
     * 用户登出
     */
    void logout();

    PostSummaryVO getPostSummary(String barId,String postId);

    /**
     * 创建评论（经 Feign 调 post-server）
     * @param postId 帖子 id
     * @param request 评论请求体
     * @return 评论 id 与楼层
     */
    CreateCommentVO createComment(String postId, CreateCommentRequest request);

    /**
     * 创建帖子（经 Feign 调 post-server）
     * @param barId 吧 id
     * @param request 帖子请求体
     * @return 帖子 id
     */
    CreatePostVO createPost(String barId, CreatePostRequest request);
}
