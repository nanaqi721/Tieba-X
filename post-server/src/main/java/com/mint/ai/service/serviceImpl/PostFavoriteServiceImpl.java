package com.mint.ai.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mint.ai.common.coontext.UserContext;
import com.mint.ai.common.enums.PostErrorCode;
import com.mint.ai.common.exception.ClientException;
import com.mint.ai.common.redisKey.RedisKeyConstant;
import com.mint.ai.mapper.PostFavoriteMapper;
import com.mint.ai.mapper.PostMapper;
import com.mint.ai.mapper.entiy.PostDO;
import com.mint.ai.mapper.entiy.PostFavoriteDO;
import com.mint.ai.service.PostFavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 帖子收藏实现层
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostFavoriteServiceImpl implements PostFavoriteService {

    private final PostMapper postMapper;

    private final PostFavoriteMapper postFavoriteMapper;

    private final StringRedisTemplate stringRedisTemplate;

    // 复用点赞的计数 lua（参数化后按字段名同步摘要缓存）
    private static final String COUNT_INCR = "lua/like_incr.lua";

    private static final DefaultRedisScript<Long> COUNT_INCR_SCRIPT = new DefaultRedisScript<>();

    static {
        COUNT_INCR_SCRIPT.setLocation(new ClassPathResource(COUNT_INCR));
        COUNT_INCR_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Long postCollect(String postId) {
        // 登录已由 LoginInterceptor 兜底，此处直接取用户 id
        String userId = UserContext.getUserId();
        // 帖子存在校验，顺便取 barId（拼摘要缓存 key）和 DB 基准计数
        PostDO postDO = postMapper.selectById(postId);
        if(postDO == null){
            throw new ClientException(PostErrorCode.POST_NOT_FOUND.getMessage(), null, PostErrorCode.POST_NOT_FOUND);
        }
        // 幂等仲裁：唯一索引 uk_user_post，只有 insert 成功才计数；重复收藏幂等返回
        try{
            postFavoriteMapper.insert(PostFavoriteDO.builder()
                    .userId(userId)
                    .postId(postId)
                    .build());
        }catch (DuplicateKeyException ex){
            return postDO.getFavoriteCount() + currentBuffer(postId);
        }
        // 更新计数缓冲 + 摘要缓存（lua 一步完成），返回最新收藏数 = DB列 + 缓冲
        String countKey = String.format(RedisKeyConstant.POST_COUNT_INCR, "favorite_count");
        String cacheKey = String.format(RedisKeyConstant.POST_CACHE_SUMMARY, postDO.getBarId(), postId);
        Long buf = stringRedisTemplate.execute(COUNT_INCR_SCRIPT,
                List.of(countKey, cacheKey),
                postId, "1", "favoriteCount");
        return postDO.getFavoriteCount() + (buf == null ? 0 : buf);
    }

    @Override
    public Long postUncollect(String postId) {
        // 登录已由 LoginInterceptor 兜底，此处直接取用户 id
        String userId = UserContext.getUserId();
        // 帖子存在校验，顺便取 barId（拼摘要缓存 key）和 DB 基准计数
        PostDO postDO = postMapper.selectById(postId);
        if(postDO == null){
            throw new ClientException(PostErrorCode.POST_NOT_FOUND.getMessage(), null, PostErrorCode.POST_NOT_FOUND);
        }
        // 删除收藏记录；从未收藏则幂等返回，不扣减
        int affected = postFavoriteMapper.delete(new LambdaQueryWrapper<PostFavoriteDO>()
                .eq(PostFavoriteDO::getUserId, userId)
                .eq(PostFavoriteDO::getPostId, postId));
        if (affected == 0) {
            return postDO.getFavoriteCount() + currentBuffer(postId);
        }
        // 更新计数缓冲 + 摘要缓存（lua 一步完成，增量为 -1），返回最新收藏数 = DB列 + 缓冲
        String countKey = String.format(RedisKeyConstant.POST_COUNT_INCR, "favorite_count");
        String cacheKey = String.format(RedisKeyConstant.POST_CACHE_SUMMARY, postDO.getBarId(), postId);
        Long buf = stringRedisTemplate.execute(COUNT_INCR_SCRIPT,
                List.of(countKey, cacheKey),
                postId, "-1", "favoriteCount");
        return postDO.getFavoriteCount() + (buf == null ? 0 : buf);
    }

    @Override
    public Boolean postCollected(String postId) {
        // 帖子存在校验
        PostDO postDO = postMapper.selectById(postId);
        if(postDO == null){
            throw new ClientException(PostErrorCode.POST_NOT_FOUND.getMessage(), null, PostErrorCode.POST_NOT_FOUND);
        }
        LambdaQueryWrapper<PostFavoriteDO> wrapper = Wrappers.lambdaQuery(PostFavoriteDO.class)
                .eq(PostFavoriteDO::getUserId, UserContext.getUserId())
                .eq(PostFavoriteDO::getPostId, postId);
        return postFavoriteMapper.selectCount(wrapper) > 0;
    }

    /**
     * 当前计数缓冲（缓冲中未刷库的增量）
     */
    private long currentBuffer(String postId) {
        Object v = stringRedisTemplate.opsForHash().get(
                String.format(RedisKeyConstant.POST_COUNT_INCR, "favorite_count"), postId);
        return v == null ? 0 : Long.parseLong(v.toString());
    }
}
