package com.mint.ai.service.impl;

import com.mint.ai.common.dto.CreateBarRequest;
import com.mint.ai.mapper.BarMapper;
import com.mint.ai.service.BarService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * bar服务层实现类
 */
@Service
@RequiredArgsConstructor
public class BarServiceImpl implements BarService {

    private final BarMapper barMapper;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Long createBar(CreateBarRequest request) {

    }
}
