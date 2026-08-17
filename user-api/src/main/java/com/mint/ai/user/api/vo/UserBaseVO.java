package com.mint.ai.user.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户简要信息（跨服务展示用：帖子作者/楼层作者昵称头像）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBaseVO {

    private String id;

    private String nickname;

    private String avatarUrl;
}
