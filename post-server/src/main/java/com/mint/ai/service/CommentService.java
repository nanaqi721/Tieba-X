package com.mint.ai.service;

import com.mint.ai.post.api.dto.CreateCommentRequest;
import com.mint.ai.post.api.vo.CreateCommentVO;
import com.mint.ai.post.api.vo.FloorPageResponseVO;
import com.mint.ai.post.api.vo.FloorVO;

import java.util.List;

/**
 * 评论服务层
 */
public interface CommentService {
    CreateCommentVO createComment(String postId, CreateCommentRequest request);

    /**
     * 删除评论（作者或楼主可删，级联删除楼中楼）
     * @param commentId 评论 id
     */
    void deleteComment(String commentId);

    /**
     * 楼层分页查询（页码分页）：本页顶层楼层 + 各自楼中楼子树，并聚合楼层作者昵称/头像
     */
    FloorPageResponseVO listFloors(String postId, Integer pageNum, Integer pageSize);

    FloorPageResponseVO listReplies(String postId, String rootId, Integer pageNum, Integer pageSize);

    /** 根据主楼或任意回复 id 查询完整楼层线程。 */
    List<FloorVO> listFloorThread(String postId, String floorId);
}
