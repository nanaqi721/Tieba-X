package com.mint.ai.mapper.entiy;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论实体
 */
@TableName("comment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String postId;

    private String userId;

    private String parentId;

    private String content;

    private Integer likeCount;

    private Integer status;

    private Integer floor;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

}
