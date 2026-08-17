package com.mint.ai.common.dto;

import com.mint.ai.common.cursor.BarPostFeedCursor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 在bar首页流式查询请求参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostFeedInBarRequest {

    /**
     * 排序字段 hot 根据热度 createTime 根据创建时间(默认值)
     */
    private String orderBy;

    private Integer pageSize;

    private BarPostFeedCursor cursor;
}
