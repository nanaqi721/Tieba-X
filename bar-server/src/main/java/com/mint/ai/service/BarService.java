package com.mint.ai.service;

import com.mint.ai.bar.api.dto.CreateBarRequest;
import com.mint.ai.bar.api.vo.BarBaseVO;
import com.mint.ai.common.vo.BarSearchResultVO;

import java.util.List;
import java.util.Map;

/**
 * bar服务层
 */
public interface BarService {
    Long createBar(CreateBarRequest request);

    void deleteBar(String barId);

    BarBaseVO queryBar(String barId);

    /**
     * 批量查询吧详情；不存在的吧降级跳过，不报错
     */
    Map<String, BarBaseVO> queryBarList(List<String> ids);

    /**
     * 关注吧，返回最新粉丝数
     */
    Long followBar(String barId);

    /**
     * 取消关注吧，返回最新粉丝数
     */
    Long unfollowBar(String barId);

    /**
     * 当前用户是否关注了该吧
     */
    Boolean isFollowed(String barId);

    BarSearchResultVO searchBars(String keyword, String cursor, Integer pageSize);
}
