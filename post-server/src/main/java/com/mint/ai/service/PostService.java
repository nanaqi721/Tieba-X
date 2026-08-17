package com.mint.ai.service;

import com.mint.ai.common.dto.UpdatePostRequest;
import com.mint.ai.post.api.dto.CreatePostRequest;
import com.mint.ai.post.api.vo.CreatePostVO;
import com.mint.ai.post.api.vo.FeedPageVO;
import com.mint.ai.post.api.vo.PostDetailPageVO;
import com.mint.ai.post.api.vo.PostSummaryVO;

/**
 * @param
 * @return
 */
public interface PostService {
    CreatePostVO createPost(String barId, CreatePostRequest request);

    void deletePostById(String barId, String postId);

    void updatePostById(String barId, UpdatePostRequest request);

    PostSummaryVO getPostSummary(String barId,String postId);

    /**
     * 帖子详情页（帖子 + 所属吧 + 作者，content 全量不截断，不含楼层）
     */
    PostDetailPageVO getPostDetail(String postId);

    /**
     * 主页帖子流（匿名）：游标分页查询 + 聚合吧信息
     */
    FeedPageVO getFeed(String cursor, Integer pageSize);
}
