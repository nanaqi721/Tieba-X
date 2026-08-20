package com.mint.ai.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 创建帖子请求体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePostRequest {


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

    @NotBlank(message = "吧id不能为空")
    private String barId;
    /**
     * 已通过文件服务上传的图片 URL，最多 5 张。
     */
    @Size(max = 5, message = "帖子图片最多上传5张")
    private List<@NotBlank(message = "图片地址不能为空") String> images;
}
