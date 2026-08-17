package com.mint.ai.post.api.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 主页帖子流返回实体
 */
@Data
@Accessors(chain = true)
public class PostFeedItemVO {
    private List<PostCardVO> records;
    private String nextCursor;
    private Boolean hasMore;
}
