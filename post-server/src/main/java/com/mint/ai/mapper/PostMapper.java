package com.mint.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mint.ai.common.cursor.BarPostFeedCursor;
import com.mint.ai.common.cursor.PostHomeFeedCursor;
import com.mint.ai.common.cursor.PostSearchCursor;
import com.mint.ai.job.PostCountFlushJob;
import com.mint.ai.mapper.entity.PostDO;
import com.mint.ai.common.vo.BarPostCardVO;
import com.mint.ai.common.vo.PostHomePageVO;
import com.mint.ai.common.vo.PostSearchItemVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * post的mapper层
 */
public interface PostMapper extends BaseMapper<PostDO> {


    int batchIncrementPostCount(@Param("metric") String metric, @Param("list")List<PostCountFlushJob.CountIncr> list);

    List<PostHomePageVO> postHomePage(@Param("cursor") PostHomeFeedCursor cursor, @Param("pageSize") Integer pageSize);

    List<BarPostCardVO> getPostFeedInBar(@Param("barId") String barId, @Param("orderBy") String orderBy, @Param("cursor")BarPostFeedCursor cursor,@Param("pageSize") Integer pageSize);

    List<PostSearchItemVO> searchPosts(@Param("keyword") String keyword,
                                       @Param("cursor") PostSearchCursor cursor,
                                       @Param("pageSize") Integer pageSize);
}
