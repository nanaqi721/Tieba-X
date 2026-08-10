package com.mint.ai.common.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 主页帖子流返回实体
 */
@Data
@Accessors(chain = true)
public class FeedPageVO {
    private List<FeedPostVO> records;
    private String nextCursor;
    private Boolean hasMore;
}
