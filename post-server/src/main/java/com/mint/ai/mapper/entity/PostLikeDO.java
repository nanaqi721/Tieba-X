package com.mint.ai.mapper.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 帖子/评论 点赞实体类
 */
@TableName("post_like")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class PostLikeDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String userId;

    private Integer targetType;

    private String targetId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
