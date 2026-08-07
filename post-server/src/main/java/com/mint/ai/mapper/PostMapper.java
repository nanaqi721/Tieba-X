package com.mint.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mint.ai.job.PostCountFlushJob;
import com.mint.ai.mapper.entiy.PostDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * post的mapper层
 */
public interface PostMapper extends BaseMapper<PostDO> {


    int batchIncrementPostCount(@Param("metric") String metric, @Param("list")List<PostCountFlushJob.CountIncr> list);
}
