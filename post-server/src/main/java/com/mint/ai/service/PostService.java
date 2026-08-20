package com.mint.ai.service;

import com.mint.ai.common.dto.PostFeedInBarRequest;
import com.mint.ai.common.dto.UpdatePostRequest;
import com.mint.ai.common.vo.PostFeedInBarVO;
import com.mint.ai.common.dto.CreatePostRequest;
import com.mint.ai.common.vo.*;

/**
 * @param
 * @return
 */
public interface PostService {
    PostCreateVO createPost(CreatePostRequest request);

    void deletePostById(String postId);

    void updatePostById(UpdatePostRequest request);

    PostSummaryVO getPostSummary(String postId);

    /**
     * 主页帖子流（匿名）：游标分页查询 + 聚合吧信息
     */
    PostFeedItemVO homePostsFeed(String cursor, Integer pageSize);

    PostFeedInBarVO getPostFeedInBar(PostFeedInBarRequest request);

    PostSearchResultVO searchPosts(String keyword, String cursor, Integer pageSize);
}
