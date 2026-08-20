package com.mint.ai.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.esotericsoftware.minlog.Log;
import com.mint.ai.common.coontext.UserContext;
import com.mint.ai.bar.api.dto.CreateBarRequest;
import com.mint.ai.common.enums.BarErrorCode;
import com.mint.ai.common.enums.BaseEnums;
import com.mint.ai.common.enums.FollowTargetType;
import com.mint.ai.common.excption.ClientException;
import com.mint.ai.common.excption.ServiceException;
import com.mint.ai.common.cursor.BarSearchCursor;
import com.mint.ai.common.redisKey.BarConstantRedisKey;
import com.mint.ai.common.vo.BarSearchItemVO;
import com.mint.ai.common.vo.BarSearchResultVO;
import com.mint.ai.bar.api.vo.BarBaseVO;
import com.mint.ai.mapper.BarMapper;
import com.mint.ai.mapper.FollowMapper;
import com.mint.ai.mapper.entity.BarDO;
import com.mint.ai.mapper.entity.FollowDO;
import com.mint.ai.service.BarService;
import com.mint.ai.utils.MyMapUtils;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * bar服务层实现类
 */
@Service
@RequiredArgsConstructor
public class BarServiceImpl implements BarService {

    private final ObjectMapper objectMapper;

    private final BarMapper barMapper;

    private final FollowMapper followMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final RedissonClient redissonClient;

    // lua脚本路径
    private static final String HSET_WITH_TTL = "lua/hset_with_ttl.lua";

    // 自动初始化脚本
    private static final DefaultRedisScript<Long> HSET_WITH_TTL_SCRIPT = new DefaultRedisScript<>();
    static {
        HSET_WITH_TTL_SCRIPT.setLocation(new ClassPathResource(HSET_WITH_TTL));
        HSET_WITH_TTL_SCRIPT.setResultType(Long.class);
    }

    // 关注数增量lua脚本
    private static final String BAR_FOLLOW_INCR = "lua/bar_follow_incr.lua";

    private static final DefaultRedisScript<Long> BAR_FOLLOW_INCR_SCRIPT = new DefaultRedisScript<>();
    static {
        BAR_FOLLOW_INCR_SCRIPT.setLocation(new ClassPathResource(BAR_FOLLOW_INCR));
        BAR_FOLLOW_INCR_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Long createBar(CreateBarRequest request) {
        BarDO bar = BeanUtil.toBean(request, BarDO.class);
        bar.setCreatorId(UserContext.getUserId());
        try {
            barMapper.insert(bar);
            // 创建成功了删除空缓存
            stringRedisTemplate.delete(String.format(BarConstantRedisKey.BAR_DETAIL_NULL_CACHE,bar.getId()));
            return Long.parseLong(bar.getId());
        } catch (DuplicateKeyException ex) {
            throw new ClientException(BarErrorCode.BAR_NAME_EXISTS.getMessage(),ex,BarErrorCode.BAR_NAME_EXISTS);

        }
    }

    @Override
    public void deleteBar(String barId) {
        String userId = UserContext.getUserId();
        int affected = barMapper.delete(
                Wrappers.lambdaUpdate(BarDO.class)
                        .eq(BarDO::getId, barId)
                        .eq(BarDO::getCreatorId, userId)
        );
        if (affected == 0) {
            throw new ClientException("吧不存在或无权删除");
        }
        stringRedisTemplate.delete(String.format(BarConstantRedisKey.BAR_DETAIL_CACHE, barId));
        stringRedisTemplate.delete(String.format(BarConstantRedisKey.BAR_DETAIL_NULL_CACHE, barId));
    }

    @Override
    public BarBaseVO queryBar(String barId) {

        Map<Object, Object> entries = stringRedisTemplate.opsForHash()
                .entries(String.format(BarConstantRedisKey.BAR_DETAIL_CACHE, barId));
        // 如果缓存存在直接返回
        if(!entries.isEmpty()){
            Map<String, String> stringMap = MyMapUtils.mapToStingMap(entries);
            BarBaseVO barDetailVO = BeanUtil.mapToBean(stringMap, BarBaseVO.class, false);
            return barDetailVO;
        }

        // 查询空缓存
        String checkNullCache = stringRedisTemplate.opsForValue().get(String.format(BarConstantRedisKey.BAR_DETAIL_NULL_CACHE, barId));

        // 如果空缓存不为null，直接抛异常
        if(checkNullCache != null){
            throw new ClientException(BarErrorCode.BAR_NOT_FOUND.getMessage(),null,BarErrorCode.BAR_NOT_FOUND);
        }

        // 缓存没有 空缓存没有 查询数据库
        // 获取锁
        RLock lock = redissonClient.getLock(String.format(BarConstantRedisKey.BAR_UPDATE_DETAIL_CACHE_LOCK, barId));
        try {
            // 抢到锁了执行逻辑
            if(lock.tryLock(2, TimeUnit.SECONDS)){

                entries = stringRedisTemplate.opsForHash()
                        .entries(String.format(BarConstantRedisKey.BAR_DETAIL_CACHE, barId));
                // 如果缓存存在直接返回
                if(!entries.isEmpty()){
                    Map<String, String> stringMap = MyMapUtils.mapToStingMap(entries);
                    BarBaseVO barDetailVO = BeanUtil.mapToBean(stringMap, BarBaseVO.class, false);
                    return barDetailVO;
                }

                // 查询空缓存
                checkNullCache = stringRedisTemplate.opsForValue().get(String.format(BarConstantRedisKey.BAR_DETAIL_NULL_CACHE, barId));

                // 如果空缓存不为null，直接抛异常
                if(checkNullCache != null){
                    throw new ClientException(BarErrorCode.BAR_NOT_FOUND.getMessage(),null,BarErrorCode.BAR_NOT_FOUND);
                }

                // 如果执行到这说明当前线程第一次构建缓存查询数据库
                BarDO barDO = barMapper.selectById(barId);
                // 数据库没有数据构建空缓存
                if(barDO == null){
                    stringRedisTemplate.opsForValue().set(String.format(BarConstantRedisKey.BAR_DETAIL_NULL_CACHE, barId)
                            , "",5,TimeUnit.MINUTES);
                    throw new ClientException(BarErrorCode.BAR_NOT_FOUND.getMessage(),null,BarErrorCode.BAR_NOT_FOUND);
                }

                // 构建缓存 返回值（postCount/followerCount 叠加未刷库增量）
                BarBaseVO build = BarBaseVO.builder()
                        .name(barDO.getName())
                        .avatarUrl(barDO.getAvatarUrl())
                        .postCount(addPendingCount(barId, "post_count", barDO.getPostCount()))
                        .followerCount(addPendingCount(barId, "follower_count", barDO.getFollowerCount()))
                        .build();
                Map<String, Object> cacheMap = BeanUtil.beanToMap(build);
                Map<String, String> stringCacheMap = new HashMap<>();
                cacheMap.forEach((k,v) -> stringCacheMap.put(k,v == null ? null : v.toString()));

                ArrayList<Object> args = new ArrayList<>();
                args.add(BarConstantRedisKey.BAR_CACHE_TTL_SECONDS);
                stringCacheMap.forEach((field, value) -> {
                    args.add(field);
                    args.add(value);
                });
                // 原子写入缓存并设置TTL
                stringRedisTemplate.execute(HSET_WITH_TTL_SCRIPT,
                        List.of(String.format(BarConstantRedisKey.BAR_DETAIL_CACHE, barId)),
                        args.toArray());
                // 删除空缓存，避免"吧已存在但空缓存未过期"导致查询误判不存在
                stringRedisTemplate.delete(String.format(BarConstantRedisKey.BAR_DETAIL_NULL_CACHE, barId));
                return build;

            }
        } catch (InterruptedException exception){
            // 恢复当前线程打断状态
            Thread.currentThread().interrupt();
            Log.error("获取锁{}时被打断",String.format(BarConstantRedisKey.BAR_UPDATE_DETAIL_CACHE_LOCK,barId));
            throw new ServiceException(BaseEnums.SYSTEM_ERROR.getMessage(),exception,BaseEnums.SYSTEM_ERROR);
        } finally {
            // 如果当前线程是获取锁的线程，释放该锁
            if(lock.isHeldByCurrentThread()){
                lock.unlock();;
            }
        }

        BarDO barDO = barMapper.selectById(barId);
        if(barDO == null){
            throw new ClientException(BarErrorCode.BAR_NOT_FOUND.getMessage(),null,BarErrorCode.BAR_NOT_FOUND);
        }
        // 兜底同样叠加未刷库增量，与获锁路径行为保持一致
        BarBaseVO detailVO = BeanUtil.toBean(barDO, BarBaseVO.class);
        detailVO.setPostCount(addPendingCount(barId, "post_count", detailVO.getPostCount()));
        detailVO.setFollowerCount(addPendingCount(barId, "follower_count", detailVO.getFollowerCount()));
        return detailVO;
    }

    @Override
    public Map<String, BarBaseVO> queryBarList(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, BarBaseVO> result = new HashMap<>();
        // 去重
        for (String barId : new LinkedHashSet<>(ids)) {
            if (StrUtil.isBlank(barId)) {
                continue;
            }
            try {
                // 复用单吧查询：缓存→空缓存→Redisson锁→建缓存→buffer叠加 全在内
                result.put(barId, queryBar(barId));
            } catch (ClientException | ServiceException e) {
                // "吧不存在"降级为跳过，由调用方兜底"未知吧"
            }
        }
        return result;
    }

    /**
     * 叠加未刷库的增量，保证 详情 = DB列 + 缓冲
     */
    private Integer addPendingCount(String barId, String metric, Integer base) {
        Object pending = stringRedisTemplate.opsForHash().get(
                String.format(BarConstantRedisKey.BAR_COUNT_INCR, metric), barId);
        return pending == null ? base : base + Integer.parseInt(pending.toString());
    }

    /**
     * 关注吧，返回最新粉丝数
     */
    @Override
    public Long followBar(String barId) {
        String userId = UserContext.getUserId();
        BarDO barDO = barMapper.selectById(barId);
        if (barDO == null) {
            throw new ClientException(BarErrorCode.BAR_NOT_FOUND.getMessage(), null, BarErrorCode.BAR_NOT_FOUND);
        }
        // 幂等仲裁：唯一索引 uk_user_target，只有 insert 成功才计数；重复关注幂等返回，不报错
        try {
            followMapper.insert(FollowDO.builder()
                    .targetType(FollowTargetType.BAR.getType())
                    .targetId(barId)
                    .userId(userId)
                    .build());
        } catch (DuplicateKeyException e) {
            return barDO.getFollowerCount() + currentFollowerBuffer(barId);
        }
        // 更新计数缓冲 + 详情缓存（lua 一步完成），返回最新粉丝数 = DB列 + 缓冲
        String countKey = String.format(BarConstantRedisKey.BAR_COUNT_INCR, "follower_count");
        String cacheKey = String.format(BarConstantRedisKey.BAR_DETAIL_CACHE, barId);
        Long buf = stringRedisTemplate.execute(BAR_FOLLOW_INCR_SCRIPT,
                List.of(countKey, cacheKey),
                barId, "1", "followerCount");
        return barDO.getFollowerCount() + (buf == null ? 0 : buf);
    }

    /**
     * 取消关注吧，返回最新粉丝数
     */
    @Override
    public Long unfollowBar(String barId) {
        String userId = UserContext.getUserId();
        BarDO barDO = barMapper.selectById(barId);
        if (barDO == null) {
            throw new ClientException(BarErrorCode.BAR_NOT_FOUND.getMessage(), null, BarErrorCode.BAR_NOT_FOUND);
        }
        // 删除关注记录，唯一索引保证不会误删他人记录；从未关注则幂等返回当前计数，不扣减
        int affected = followMapper.delete(new LambdaQueryWrapper<FollowDO>()
                .eq(FollowDO::getUserId, userId)
                .eq(FollowDO::getTargetType, FollowTargetType.BAR.getType())
                .eq(FollowDO::getTargetId, barId));
        if (affected == 0) {
            return barDO.getFollowerCount() + currentFollowerBuffer(barId);
        }
        // 更新计数缓冲 + 详情缓存（lua 一步完成，增量为 -1）
        String countKey = String.format(BarConstantRedisKey.BAR_COUNT_INCR, "follower_count");
        String cacheKey = String.format(BarConstantRedisKey.BAR_DETAIL_CACHE, barId);
        Long buf = stringRedisTemplate.execute(BAR_FOLLOW_INCR_SCRIPT,
                List.of(countKey, cacheKey),
                barId, "-1", "followerCount");
        return barDO.getFollowerCount() + (buf == null ? 0 : buf);
    }

    /**
     * 当前用户是否关注了该吧
     */
    @Override
    public Boolean isFollowed(String barId) {
        BarDO barDO = barMapper.selectById(barId);
        if (barDO == null) {
            throw new ClientException(BarErrorCode.BAR_NOT_FOUND.getMessage(), null, BarErrorCode.BAR_NOT_FOUND);
        }
        LambdaQueryWrapper<FollowDO> wrapper = Wrappers.lambdaQuery(FollowDO.class)
                .eq(FollowDO::getUserId, UserContext.getUserId())
                .eq(FollowDO::getTargetType, FollowTargetType.BAR.getType())
                .eq(FollowDO::getTargetId, barId);
        Long count = followMapper.selectCount(wrapper);
        return count > 0 ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * 当前粉丝数缓冲（缓冲中未刷库的增量）
     */
    private long currentFollowerBuffer(String barId) {
        Object v = stringRedisTemplate.opsForHash().get(
                String.format(BarConstantRedisKey.BAR_COUNT_INCR, "follower_count"), barId);
        return v == null ? 0 : Long.parseLong(v.toString());
    }

    @Override
    public BarSearchResultVO searchBars(String keyword, String cursor, Integer pageSize) {
        if (StrUtil.isBlank(keyword)) {
            throw new ClientException("贴吧搜索词不能为空");
        }
        String normalizedKeyword = keyword.strip();
        if (normalizedKeyword.length() > 20) {
            throw new ClientException("贴吧搜索词长度必须在1到20之间");
        }
        if (pageSize != null && (pageSize < 1 || pageSize > 10)) {
            throw new ClientException("pageSize必须在1到10之间");
        }
        int size = pageSize == null ? 10 : pageSize;
        String keywordDigest = DigestUtil.sha256Hex(normalizedKeyword.toLowerCase(Locale.ROOT));
        BarSearchCursor pageCursor = decodeSearchCursor(cursor, keywordDigest);
        String escapedKeyword = normalizedKeyword
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        List<BarSearchItemVO> records = barMapper.searchBars(escapedKeyword, pageCursor, size + 1);
        boolean hasMore = records.size() > size;
        if (hasMore) {
            records = new ArrayList<>(records.subList(0, size));
        }

        String nextCursor = null;
        if (hasMore) {
            BarSearchItemVO last = records.get(records.size() - 1);
            nextCursor = encodeSearchCursor(BarSearchCursor.builder()
                    .followerCount(last.getFollowerCount())
                    .postCount(last.getPostCount())
                    .barId(last.getBarId())
                    .keywordDigest(keywordDigest)
                    .build());
        }
        return BarSearchResultVO.builder()
                .records(records)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }

    private BarSearchCursor decodeSearchCursor(String cursor, String keywordDigest) {
        if (StrUtil.isBlank(cursor)) {
            return null;
        }
        try {
            byte[] json = Base64.getUrlDecoder().decode(cursor);
            BarSearchCursor pageCursor = objectMapper.readValue(json, BarSearchCursor.class);
            if (pageCursor.getFollowerCount() == null
                    || pageCursor.getPostCount() == null
                    || StrUtil.isBlank(pageCursor.getBarId())
                    || StrUtil.isBlank(pageCursor.getKeywordDigest())) {
                throw new ClientException("游标无效");
            }
            if (!keywordDigest.equals(pageCursor.getKeywordDigest())) {
                throw new ClientException("游标与贴吧搜索词不匹配");
            }
            return pageCursor;
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            throw new ClientException("游标无效", e);
        }
    }

    private String encodeSearchCursor(BarSearchCursor cursor) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(cursor);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception e) {
            throw new ServiceException("生成贴吧搜索游标失败", e);
        }
    }
}
