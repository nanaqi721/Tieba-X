package com.mint.ai.mapper.entiy;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 帖子收藏实体类
 */
@TableName("favorite")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostFavoriteDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String userId;

    private String postId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
