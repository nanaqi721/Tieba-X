package com.mint.ai.mapper.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 关注实体类（target_type 区分关注吧 / 关注用户）
 */
@TableName("follow")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class FollowDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String userId;

    private Integer targetType;

    private String targetId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
