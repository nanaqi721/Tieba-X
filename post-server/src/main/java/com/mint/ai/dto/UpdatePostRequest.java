package com.mint.ai.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新帖子
 */
@Data
public class UpdatePostRequest {

    /**
     * 帖子id
     */
    private String postId;

    /**
     * 修改标题
     */
    @Size(min = 5,max = 30,message = "帖子标题长度需在5-30之间")
    private String title;

    /**
     * 修改内容
     */
    private String content;
}
