package com.mint.ai.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mint.ai.common.coontext.UserContext;
import com.mint.ai.common.enums.LikeTargetType;
import com.mint.ai.common.enums.PostErrorCode;
import com.mint.ai.common.exception.ClientException;
import com.mint.ai.common.redisKey.RedisKeyConstant;
import com.mint.ai.mapper.PostLikeMapper;
import com.mint.ai.mapper.PostMapper;
import com.mint.ai.mapper.entiy.PostDO;
import com.mint.ai.mapper.entiy.PostLikeDO;
import com.mint.ai.service.PostLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 帖子点赞业务实现类
 */
@Service
@RequiredArgsConstructor
public class PostLikeServiceImpl implements PostLikeService {

    private final PostMapper postMapper;

    private final PostLikeMapper postLikeMapper;

    private final StringRedisTemplate stringRedisTemplate;

    // lua脚本路径
    private static final String LIKE_INCR = "lua/like_incr.lua";

    // 自动初始化脚本
    private static final DefaultRedisScript<Long> LIKE_INCR_SCRIPT = new DefaultRedisScript<>();

    static {
        LIKE_INCR_SCRIPT.setLocation(new ClassPathResource(LIKE_INCR));
        LIKE_INCR_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Long postLike(String postId) {
        // 登录已由 LoginInterceptor 兜底（DB 层 user_id NOT NULL 防 null 绕过唯一索引），此处直接取用户 id
        String userId = UserContext.getUserId();
        // 帖子存在校验，顺便取 barId（拼摘要缓存 key）和 DB 基准计数
        PostDO postDO = postMapper.selectById(postId);
        if (postDO == null) {
            throw new ClientException(PostErrorCode.POST_NOT_FOUND.getMessage(), null, PostErrorCode.POST_NOT_FOUND);
        }
        // 幂等仲裁：唯一索引，只有 insert 成功才计数；重复点赞幂等返回，不报错
        try {
            postLikeMapper.insert(PostLikeDO.builder()
                    .targetType(LikeTargetType.POST.getType())
                    .targetId(postId)
                    .userId(userId)
                    .build());
        } catch (DuplicateKeyException e) {
            return postDO.getLikeCount() + currentBuffer(postId);
        }
        // 更新计数缓冲 + 摘要缓存（lua 一步完成），返回最新点赞数 = DB列 + 缓冲
        String countKey = String.format(RedisKeyConstant.POST_COUNT_INCR, "like_count");
        String cacheKey = String.format(RedisKeyConstant.POST_CACHE_SUMMARY, postDO.getBarId(), postId);
        Long buf = stringRedisTemplate.execute(LIKE_INCR_SCRIPT,
                List.of(countKey, cacheKey),
                postId, "1", "likeCount");
        // 拿的是插入前检查的点赞量即使在此期间有人点赞也不会扰乱计数
        return postDO.getLikeCount() + (buf == null ? 0 : buf);
    }

    @Override
    public Long postUnlike(String postId) {
        // 登录已由 LoginInterceptor 兜底，此处直接取用户 id
        String userId = UserContext.getUserId();
        // 帖子存在校验，顺便取 barId 和 DB 基准计数
        PostDO postDO = postMapper.selectById(postId);
        if (postDO == null) {
            throw new ClientException(PostErrorCode.POST_NOT_FOUND.getMessage(), null, PostErrorCode.POST_NOT_FOUND);
        }
        // 删除点赞记录，唯一索引保证不会误删他人记录；从未点赞则幂等返回当前计数，不扣减
        int affected = postLikeMapper.delete(new LambdaQueryWrapper<PostLikeDO>()
                .eq(PostLikeDO::getUserId, userId)
                .eq(PostLikeDO::getTargetType, LikeTargetType.POST.getType())
                .eq(PostLikeDO::getTargetId, postId));
        if (affected == 0) {
            return postDO.getLikeCount() + currentBuffer(postId);
        }
        // 更新计数缓冲 + 摘要缓存（lua 一步完成，增量为 -1）
        String countKey = String.format(RedisKeyConstant.POST_COUNT_INCR, "like_count");
        String cacheKey = String.format(RedisKeyConstant.POST_CACHE_SUMMARY, postDO.getBarId(), postId);
        Long buf = stringRedisTemplate.execute(LIKE_INCR_SCRIPT,
                List.of(countKey, cacheKey),
                postId, "-1", "likeCount");
        return postDO.getLikeCount() + (buf == null ? 0 : buf);
    }

    @Override
    public Boolean postLiked(String postId) {
        // 帖子存在校验
        PostDO postDO = postMapper.selectById(postId);
        if (postDO == null) {
            throw new ClientException(PostErrorCode.POST_NOT_FOUND.getMessage(), null, PostErrorCode.POST_NOT_FOUND);
        }
        LambdaQueryWrapper<PostLikeDO> wrapper = Wrappers.lambdaQuery(PostLikeDO.class)
                .eq(PostLikeDO::getUserId, UserContext.getUserId())
                .eq(PostLikeDO::getTargetType, LikeTargetType.POST.getType())
                .eq(PostLikeDO::getTargetId, postId);
        Long count = postLikeMapper.selectCount(wrapper);

        return count >0 ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * 当前计数缓冲（缓冲中未刷库的增量）
     */
    private long currentBuffer(String postId) {
        Object v = stringRedisTemplate.opsForHash().get(
                String.format(RedisKeyConstant.POST_COUNT_INCR, "like_count"), postId);
        return v == null ? 0 : Long.parseLong(v.toString());
    }

}
