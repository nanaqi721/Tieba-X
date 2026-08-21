package com.mint.ai.bar.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * bar的信息返回实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BarBaseVO {

    private String name;

    private String avatarUrl;

    private Integer postCount;

    private Integer followerCount;

    private String creatorId;
}
