package com.mint.ai.common.vo;

import lombok.Data;

import java.util.List;

/**
 * 楼层分页返回（前端）：本页顶层楼层 + 分页信息
 */
@Data
public class FloorPageResponseVO {
    private List<FloorVO> records;
    private Long total;
    private Long totalPages;
    private Long pageNum;
    private Long pageSize;
}
