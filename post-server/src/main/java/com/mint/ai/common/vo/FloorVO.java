package com.mint.ai.common.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 楼层节点（前端展示）：楼层字段 + 楼层作者 + 楼中楼子树
 */
@Data
public class FloorVO {
    private String id;
    private Integer floor;
    private String userId;
    private String parentId;
    private String rootId;
    private String nickname;    // 楼层作者昵称，兜底 "未知用户"
    private String avatarUrl;
    private String content;
    private Integer likeCount;
    private LocalDateTime createTime;
    private Integer replyCount;
    private List<String> images;
    private List<FloorVO> children;
}
