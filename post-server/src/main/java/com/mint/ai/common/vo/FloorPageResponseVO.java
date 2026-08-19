package com.mint.ai.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 楼层分页返回（前端）：本页顶层楼层 + 分页信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FloorPageResponseVO {
    private List<FloorVO> records;
    // 评论总数
    private Long total;
    // 总共多少页
    private Long totalPages;
    // 当前页
    private Long pageNum;
    // 页大小
    private Long pageSize;
}
