package com.mint.ai.common.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子详情页：帖子 + 所属吧 + 作者（不含楼层，楼层走 /v1/floors 分页）
 */
@Data
public class PostDetailPageVO {
    private String postId;
    private String barId;
    private String barName;       // 兜底 "未知吧"
    private String barAvatarUrl;
    private String title;
    private String content;
    private String coverImage;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
    private Integer lastFloor;
    // 作者
    private String authorUserId;
    private String authorNickname;  // 兜底 "未知用户"
    private String authorAvatarUrl;
    private LocalDateTime createTime;  // 发帖时间
}
