package com.mint.ai.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarSearchItemVO {

    private String barId;

    private String name;

    private String avatarUrl;

    private Integer followerCount;

    private Integer postCount;
}
