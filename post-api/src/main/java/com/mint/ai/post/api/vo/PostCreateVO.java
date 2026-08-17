package com.mint.ai.post.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 帖子响应实体
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class PostCreateVO {

    /**
     * 帖子id
     */
    private String id;
    // 测试贡献
}
