package com.mint.ai.common.cursor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 主页帖子游标
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostHomePageCursor {

    /**
     * 防止同一个分数的在同10 11 然后下一轮就不查11了
     */
    private String postId;

    /**
     * 最后一条帖子的热度
     */
    private Integer score;
}
