package com.mint.ai.post.api.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子详情：content 为全量不截断
 */
@Data
public class PostDetailVO {
    private String id;
    private String barId;
    private String userId;
    private String title;
    private String content;
    private String coverImage;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
    private Integer lastFloor;
    private LocalDateTime createTime;
}
