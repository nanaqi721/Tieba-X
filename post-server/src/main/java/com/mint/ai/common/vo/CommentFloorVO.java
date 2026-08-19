package com.mint.ai.common.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 楼层节点：顶层楼层或楼中楼，children 挂楼中楼子树
 */
@Data
public class CommentFloorVO {
    private String id;
    private String postId;
    private String userId;
    /** 为空=顶层楼层；非空=楼中楼 */
    private String parentId;
    /** 顶层为 1,2,3...，楼中楼为 0 */
    private Integer floor;
    private String content;
    private Integer likeCount;
    private LocalDateTime createTime;
    private List<CommentFloorVO> children;
}
