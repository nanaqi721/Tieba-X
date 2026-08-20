package com.mint.ai.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostSearchItemVO {

    private String postId;

    private String barId;

    private String barName;

    private String barAvatarUrl;

    private String title;

    private String content;

    private LocalDateTime createTime;
}
