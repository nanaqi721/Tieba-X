package com.mint.ai.common.dto;

import lombok.Data;

/**
 * 创建bar的实体类
 */
@Data
public class CreateBarRequest {

    private String name;

    private String description;

    private String avatarUrl;
}
