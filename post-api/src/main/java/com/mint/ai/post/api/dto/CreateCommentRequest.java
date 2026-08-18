package com.mint.ai.post.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建评论请求体
 */
@Data
public class CreateCommentRequest {

    /**
     * 父评论 id，空表示顶层评论
     */
    private String parentId;


    /**
     * 评论内容
     */
    @NotBlank
    @Size(max = 1000)
    private String content;

    /**
     * 图片 URL 列表（先经上传接口获取），最多 9 张
     */
    @Size(max = 9)
    private List<String> images;
}
