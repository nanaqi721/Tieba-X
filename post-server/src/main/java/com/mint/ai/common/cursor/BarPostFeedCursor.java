package com.mint.ai.common.cursor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 吧主页滚动查询的
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BarPostFeedCursor {

    /**
     * 下一个postId
     */
    private String postId;

    /**
     * 热度排序
     */
    private Integer score;

    /**
     * 创建时间排序
     */
    private LocalDateTime createTime;

}
