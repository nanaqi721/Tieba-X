package com.mint.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mint.ai.job.CommentCountFlushJob;
import com.mint.ai.mapper.entity.CommentDO;
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
}
