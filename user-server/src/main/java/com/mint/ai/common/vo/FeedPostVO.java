package com.mint.ai.common.vo;

import lombok.Data;

/**
 * 主页帖子卡片：帖子字段 + 所属吧详情（兜底）
 */
@Data
public class FeedPostVO {
    private String postId;
    private String title;
    private String content;
    private String coverImage;
    private String barId;
    private Integer hotScore;
    // 吧详情（bar-server 缺失时兜底）
    private String barName;          // 兜底 "未知吧"
    private String barAvatarUrl;
    private Integer barPostCount;    // 兜底 0
    private Integer barFollowerCount;// 兜底 0
}
