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
public class BarSearchResultVO {

    private List<BarSearchItemVO> records;

    private String nextCursor;

    private Boolean hasMore;
}
