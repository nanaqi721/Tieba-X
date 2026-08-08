package com.mint.ai.mapper.entiy;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 图片附件实体类（帖子/评论通用）
 */
@TableName("attachment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachmentDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 业务类型 1帖子 2评论
     */
    private Integer bizType;

    /**
     * 业务 id(post.id / comment.id)
     */
    private String bizId;

    /**
     * 文件访问地址
     */
    private String url;

    private String fileName;

    private Integer fileSize;

    private Integer width;

    private Integer height;

    /**
     * 排序，越小越靠前
     */
    private Integer sortOrder;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
