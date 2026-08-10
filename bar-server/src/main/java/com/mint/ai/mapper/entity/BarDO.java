package com.mint.ai.mapper.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * bar实体类
 */
@TableName("bar")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BarDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    @NotBlank
    @Size(min = 2,max = 50, message = "吧名在2-50个字之间")
    private String name;

    private String description;

    private String avatarUrl;

    private String creatorId;

    private Integer status;

    private Integer postCount;

    private Integer followerCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

}
