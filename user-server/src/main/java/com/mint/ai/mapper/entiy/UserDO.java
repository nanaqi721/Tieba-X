package com.mint.ai.mapper.entiy;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 用户实体·
 */
@TableName("user")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String username;

    private String passwordHash;

    private String nickname;

    private String email;

    private String bio;

    private Integer sex;

    private String avatarUrl;

    private Integer role;

    private Integer status;

    private Integer postCount;

    private Integer commentCount;

    private Integer followerCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
