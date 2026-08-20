package com.mint.ai.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostSearchResultVO {

    private List<PostSearchItemVO> records;

    private String nextCursor;

    private Boolean hasMore;
}
