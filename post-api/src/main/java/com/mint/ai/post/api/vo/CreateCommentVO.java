package com.mint.ai.post.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建评论返回类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCommentVO {

    private String id;

    /**
     * 楼层：顶层评论为真实楼层，楼中楼回复为 0
     */
    private Integer floor;

    /**
     * 评论总数（创建后最新值 = DB列 + 缓冲）
     */
    private Integer commentCount;
}
