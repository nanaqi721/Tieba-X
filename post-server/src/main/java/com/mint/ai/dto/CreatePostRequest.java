package com.mint.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建帖子请求体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePostRequest {

    /**
     * 吧id
     */
    private String barId;

    /**
     * 标题
     */
    @NotBlank(message = "帖子标题不能为空")
    @Size(min = 5,max = 30,message = "帖子标题长度需在5-30之间")
    private String title;

    /**
     * 内容
     */
    @NotBlank(message = "帖子内容不能为空")
    private String content;
    // todo:上传图片
    // list图片地址
}
