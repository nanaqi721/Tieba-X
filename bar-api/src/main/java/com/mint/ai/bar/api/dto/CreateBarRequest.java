package com.mint.ai.bar.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建bar的实体类
 */
@Data
public class CreateBarRequest {

    @NotBlank(message = "吧名不能为空")
    @Size(min = 2, max = 50, message = "吧名长度需在 2-50 之间")
    private String name;

    private String description;

    private String avatarUrl;
}
