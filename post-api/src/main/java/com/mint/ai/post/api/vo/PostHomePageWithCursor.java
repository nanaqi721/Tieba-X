package com.mint.ai.post.api.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 主页帖子返回实体 带游标
 */
@Data
@Accessors(chain = true)
public class PostHomePageWithCursor {

    private List<PostHomePageVO> postHomePageVOS;

    private String nextCursor;

    /**
     * 是否还有下一页
     */
    private Boolean hasMore;
}
