package com.mint.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mint.ai.job.BarCountFlushJob;
import com.mint.ai.mapper.entity.BarDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * bar数据层
 */
public interface BarMapper extends BaseMapper<BarDO> {

    int batchIncrementCount(@Param("metric") String metric, @Param("list") List<BarCountFlushJob.CountIncr> list);
}
