package com.mint.ai.common.vo;

import lombok.Data;

/**
 * 帖子在主页分页查询返回实体
 */
@Data
public class PostHomePageVO {

    private String title;

    private String content;

    private String coverImage;

    private String barId;

    private String postId;

    /**
     * 热度分(like*5 + view*2 + comment*3),由 SQL 计算列带出,用于下一页游标
     */
    private Integer hotScore;
}
