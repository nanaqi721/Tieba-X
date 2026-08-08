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
}
