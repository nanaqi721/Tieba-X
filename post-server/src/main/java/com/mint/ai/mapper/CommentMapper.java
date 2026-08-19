package com.mint.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mint.ai.job.CommentCountFlushJob;
import com.mint.ai.mapper.entity.CommentDO;
import com.mint.ai.common.vo.FloorVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评论数据层
 */
public interface CommentMapper extends BaseMapper<CommentDO> {

    /**
     * 批量累加评论点赞数
     */
    int batchIncrementCommentCount(@Param("list") List<CommentCountFlushJob.CountIncr> list);

    int incrementRootReplyCount(@Param("postId") String postId, @Param("rootId") String rootId);

    int decrementRootReplyCount(@Param("postId") String postId, @Param("rootId") String rootId);

    /**
     * 分页查询顶层楼，并为每个顶层楼返回指定数量的最新回复。
     */
    List<FloorVO> selectFloorsWithReplies(@Param("postId") String postId,
                                          @Param("offset") long offset,
                                          @Param("pageSize") int pageSize,
                                          @Param("replyLimit") int replyLimit);

    /**
     * 查询帖子顶层楼总数。
     */
    Long countTopFloors(@Param("postId") String postId);

}
