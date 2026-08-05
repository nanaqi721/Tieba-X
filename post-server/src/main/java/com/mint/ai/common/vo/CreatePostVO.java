package com.mint.ai.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 帖子响应实体
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class CreatePostVO {

    /**
     * 帖子id
     */
    private String id;
    // 测试贡献
}
