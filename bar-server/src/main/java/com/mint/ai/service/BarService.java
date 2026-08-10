package com.mint.ai.service;

import com.mint.ai.common.dto.CreateBarRequest;

/**
 * bar服务层
 */
public interface BarService {
    Long createBar(CreateBarRequest request);
}
