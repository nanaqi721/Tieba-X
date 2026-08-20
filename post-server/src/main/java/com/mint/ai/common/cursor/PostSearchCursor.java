package com.mint.ai.common.cursor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostSearchCursor {

    private String postId;

    private LocalDateTime createTime;

    private String keywordDigest;
}
