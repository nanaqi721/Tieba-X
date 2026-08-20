package com.mint.ai.common.cursor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarSearchCursor {

    private Integer followerCount;

    private Integer postCount;

    private String barId;

    private String keywordDigest;
}
