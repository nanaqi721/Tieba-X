package com.mint.ai.service;

import com.mint.ai.common.dto.PostFeedInBarRequest;
import com.mint.ai.common.dto.UpdatePostRequest;
import com.mint.ai.common.vo.PostFeedInBarVO;
import com.mint.ai.post.api.dto.CreatePostRequest;
import com.mint.ai.post.api.vo.*;

import java.util.List;

/**
 * @param
 * @return
 */
public interface PostService {
    PostCreateVO createPost(String barId, CreatePostRequest request);

    void deletePostById(String barId, String postId);

    void updatePostById(String barId, UpdatePostRequest request);

    PostSummaryVO getPostSummary(String barId,String postId);

    /**
     * 主页帖子流（匿名）：游标分页查询 + 聚合吧信息
     */
    PostFeedItemVO getFeed(String cursor, Integer pageSize);

    PostFeedInBarVO getPostFeedInBar(String barId, PostFeedInBarRequest request);
}
