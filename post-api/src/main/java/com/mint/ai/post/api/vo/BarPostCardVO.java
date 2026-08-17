package com.mint.ai.post.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 在吧内帖子的展示卡片
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarPostCardVO {

    private String postId;

    private String title;

    private String content;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    // 最后回复时间
    private LocalDateTime lastReplyTime;

    private LocalDateTime createTime;

    private Integer hotScore;

    private String userId;

    private String nickName;

    private String avatarUrl;

}
