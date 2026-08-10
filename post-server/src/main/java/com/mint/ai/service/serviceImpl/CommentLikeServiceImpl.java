package com.mint.ai.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mint.ai.common.coontext.UserContext;
import com.mint.ai.common.enums.CommentErrorCode;
import com.mint.ai.common.enums.LikeTargetType;
import com.mint.ai.common.exception.ClientException;
import com.mint.ai.common.redisKey.RedisKeyConstant;
import com.mint.ai.mapper.CommentMapper;
import com.mint.ai.mapper.PostLikeMapper;
import com.mint.ai.mapper.entity.CommentDO;
import com.mint.ai.mapper.entity.PostLikeDO;
import com.mint.ai.service.CommentLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 评论点赞业务实现类
 */
@Service
@RequiredArgsConstructor
public class CommentLikeServiceImpl implements CommentLikeService {

    private final CommentMapper commentMapper;

    private final PostLikeMapper postLikeMapper;

    private final StringRedisTemplate stringRedisTemplate;

    // 评论点赞计数缓冲 hash key（与帖子 like_count 分开，避免 id 混淆）
    private static final String COMMENT_LIKE_COUNT = "comment_like_count";

    @Override
    public Long like(String commentId) {
        // 登录已由 LoginInterceptor 兜底，此处直接取用户 id
        String userId = UserContext.getUserId();
        // 评论存在校验，顺便取 DB 基准点赞数
        CommentDO commentDO = getCommentOrThrow(commentId);
        // 幂等仲裁：唯一索引，只有 insert 成功才计数；重复点赞幂等返回，不报错
        try {
            postLikeMapper.insert(PostLikeDO.builder()
                    .targetType(LikeTargetType.COMMENT.getType())
                    .targetId(commentId)
                    .userId(userId)
                    .build());
        } catch (DuplicateKeyException e) {
            return commentDO.getLikeCount() + currentBuffer(commentId);
        }
        // 更新计数缓冲（评论无摘要缓存，无需 lua 同步），返回最新点赞数 = DB列 + 缓冲
        Long buf = stringRedisTemplate.opsForHash().increment(String.format(RedisKeyConstant.COMMENT_COUNT_INCR,COMMENT_LIKE_COUNT), commentId, 1);
        return commentDO.getLikeCount() + (buf == null ? 0 : buf);
    }

    @Override
    public Long unlike(String commentId) {
        // 登录已由 LoginInterceptor 兜底，此处直接取用户 id
        String userId = UserContext.getUserId();
        // 评论存在校验，顺便取 DB 基准点赞数
        CommentDO commentDO = getCommentOrThrow(commentId);
        // 删除点赞记录，唯一索引保证不会误删他人记录；从未点赞则幂等返回当前计数，不扣减
        int affected = postLikeMapper.delete(new LambdaQueryWrapper<PostLikeDO>()
                .eq(PostLikeDO::getUserId, userId)
                .eq(PostLikeDO::getTargetType, LikeTargetType.COMMENT.getType())
                .eq(PostLikeDO::getTargetId, commentId));
        if (affected == 0) {
            return commentDO.getLikeCount() + currentBuffer(commentId);
        }
        // 更新计数缓冲，返回最新点赞数 = DB列 + 缓冲
        Long buf = stringRedisTemplate.opsForHash().increment(String.format(RedisKeyConstant.COMMENT_COUNT_INCR,COMMENT_LIKE_COUNT), commentId, -1);
        return commentDO.getLikeCount() + (buf == null ? 0 : buf);
    }

    @Override
    public Boolean liked(String commentId) {
        // 评论存在校验
        getCommentOrThrow(commentId);
        Long count = postLikeMapper.selectCount(Wrappers.lambdaQuery(PostLikeDO.class)
                .eq(PostLikeDO::getUserId, UserContext.getUserId())
                .eq(PostLikeDO::getTargetType, LikeTargetType.COMMENT.getType())
                .eq(PostLikeDO::getTargetId, commentId));
        return count > 0;
    }


    /**
     * 当前计数缓冲（缓冲中未刷库的增量）
     */
    private long currentBuffer(String commentId) {
        Object v = stringRedisTemplate.opsForHash().get(String.format(RedisKeyConstant.COMMENT_COUNT_INCR,COMMENT_LIKE_COUNT), commentId);
        return v == null ? 0 : Long.parseLong(v.toString());
    }

    /**
     * 评论存在性校验
     */
    private CommentDO getCommentOrThrow(String commentId) {
        CommentDO commentDO = commentMapper.selectById(commentId);
        if (commentDO == null) {
            throw new ClientException(CommentErrorCode.COMMENT_NOT_FOUND.getMessage(), null, CommentErrorCode.COMMENT_NOT_FOUND);
        }
        return commentDO;
    }

}
