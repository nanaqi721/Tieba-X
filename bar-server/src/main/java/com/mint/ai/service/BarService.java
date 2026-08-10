package com.mint.ai.service;

import com.mint.ai.bar.api.dto.CreateBarRequest;
import com.mint.ai.bar.api.vo.BarDetailVO;

/**
 * bar服务层
 */
public interface BarService {
    Long createBar(CreateBarRequest request);

    BarDetailVO queryBar(String barId);

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
