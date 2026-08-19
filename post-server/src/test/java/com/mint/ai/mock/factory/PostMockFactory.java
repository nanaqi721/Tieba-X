package com.mint.ai.mock.factory;

import com.mint.ai.common.redisKey.RedisConstantKey;
import com.mint.ai.mapper.PostMapper;
import com.mint.ai.mapper.entity.PostDO;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * post数据模拟构造工场
 */
@Component
public class PostMockFactory {
    @Resource
    private PostMapper postMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /** 造一个帖子，返回 postId */
    public String createPost(String title, String content) {
        PostDO post = PostDO.builder()
                .bar_id("1001").user_id("1001")
                .title(title).content(content)
                .viewCount(0).commentCount(0).favoriteCount(0).likeCount(0)
                .build();
        postMapper.insert(post);
        return post.getId();
    }

    /** 模拟浏览增量：HINCRBY 增量 hash */
    public void mockViewCount(String postId, long delta) {
        incrementCount(postId, "view_count", delta);
    }

    /** 模拟点赞增量 */
    public void mockLikeCount(String postId, long delta) {
        incrementCount(postId, "like_count", delta);
    }

    /** 模拟收藏增量 */
    public void mockFavoriteCount(String postId, long delta) {
        incrementCount(postId, "favorite_count", delta);
    }

    private void incrementCount(String postId, String metric, long delta) {
        stringRedisTemplate.opsForHash()
                .increment(String.format(RedisConstantKey.POST_COUNT_INCR, metric), postId, delta);
    }
}
