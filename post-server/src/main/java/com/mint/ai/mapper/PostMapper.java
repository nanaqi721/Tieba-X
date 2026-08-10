package com.mint.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mint.ai.common.cursor.PostHomePageCursor;
import com.mint.ai.job.PostCountFlushJob;
import com.mint.ai.mapper.entity.PostDO;
import com.mint.ai.post.api.vo.PostHomePageVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * post的mapper层
 */
public interface PostMapper extends BaseMapper<PostDO> {


    int batchIncrementPostCount(@Param("metric") String metric, @Param("list")List<PostCountFlushJob.CountIncr> list);

    List<PostHomePageVO> postHomePage(@Param("cursor") PostHomePageCursor cursor, @Param("pageSize") Integer pageSize);
}
