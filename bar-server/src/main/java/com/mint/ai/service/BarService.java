package com.mint.ai.service;

import com.mint.ai.bar.api.dto.CreateBarRequest;
import com.mint.ai.bar.api.vo.BarDetailVO;

import java.util.List;
import java.util.Map;

/**
 * bar服务层
 */
public interface BarService {
    Long createBar(CreateBarRequest request);

    BarDetailVO queryBar(String barId);

    /**
     * 批量查询吧详情；不存在的吧降级跳过，不报错
     */
    Map<String, BarDetailVO> queryBarList(List<String> ids);

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
}
