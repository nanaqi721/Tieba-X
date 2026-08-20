package com.mint.ai.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子摘要类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostSummaryVO {

    private String id;

    private String barId;

    private String title;

    private String content;

    private String coverImage;

    private List<String> images;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private Integer favoriteCount;

    private LocalDateTime createTime;
}
