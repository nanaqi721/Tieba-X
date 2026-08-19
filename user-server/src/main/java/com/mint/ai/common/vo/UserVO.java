package com.mint.ai.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户返回实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVO {

    private String id;

    private String username;

    private String passwordHash;

    private String nickname;

    private Integer sex;

    private String avatarUrl;

    private Integer postCount;

    private Integer commentCount;

    private Integer followerCount;
}
