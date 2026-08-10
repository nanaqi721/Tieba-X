package com.mint.ai.post.api.vo;

import lombok.Data;

import java.util.List;

/**
 * 楼层分页结果：本页顶层楼层（每层带 children），total 为顶层楼层总数
 */
@Data
public class FloorPageVO {
    private List<CommentFloorVO> floors;
    private Long total;
    private Long totalPages;
    private Long pageNum;
    private Long pageSize;
}
