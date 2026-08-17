package com.mint.ai.common.vo;

import com.mint.ai.common.cursor.BarPostFeedCursor;
import com.mint.ai.post.api.vo.BarPostCardVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 在吧主页流式查询帖子的返回实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostFeedInBarVO {

    private BarPostFeedCursor cursor;

    private List<BarPostCardVO> data;

    private Boolean hasNext;
}
