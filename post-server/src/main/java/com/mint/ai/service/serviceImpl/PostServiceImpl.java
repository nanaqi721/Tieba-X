package com.mint.ai.service.serviceImpl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.esotericsoftware.minlog.Log;
import com.mint.ai.common.coontext.UserContext;
import com.mint.ai.common.enums.BaseEnums;
import com.mint.ai.common.enums.PostErrorCode;
import com.mint.ai.common.exception.ClientException;
import com.mint.ai.common.exception.ServiceException;
import com.mint.ai.common.redisKey.RedisKeyConstant;
import com.mint.ai.common.dto.UpdatePostRequest;
import com.mint.ai.mapper.PostMapper;
import com.mint.ai.post.api.dto.CreatePostRequest;
import com.mint.ai.post.api.vo.CreatePostVO;
import com.mint.ai.post.api.vo.PostSummaryVO;
import com.mint.ai.mapper.entity.PostDO;
import com.mint.ai.service.PostService;
import com.mint.ai.utils.MyMapUtils;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 帖子控制层
 */
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final RedissonClient redissonClient;

    // lua脚本路径
    private static final String HSET_WITH_TTL = "lua/hset_with_ttl.lua";

    // 帖子缓存过期时间
    private static final String POST_CACHE_TTL_SECONDS = "3600";

    // 自动初始化脚本
    private static final DefaultRedisScript<Long> HSET_WITH_TTL_SCRIPT = new DefaultRedisScript<>();
    static {
        HSET_WITH_TTL_SCRIPT.setLocation(new ClassPathResource(HSET_WITH_TTL));
        HSET_WITH_TTL_SCRIPT.setResultType(Long.class);
    }
    @Override
    public CreatePostVO createPost(String barId, CreatePostRequest request) {

        // 登录已由 LoginInterceptor 兜底，此处直接取用户 id
        String userId = UserContext.getUserId();
        PostDO post = PostDO.builder()
                .barId("1001")
                .userId(userId)
                .title(request.getTitle())
                .content(request.getContent())
                .viewCount(0)
                .commentCount(0)
                .favoriteCount(0)
                .likeCount(0)
                .build();
        postMapper.insert(post);

        // 构建hset帖子缓存数据
        Map<String, String> cachePost = createCachePost(post.getId(), request,barId);
        ArrayList<Object> args = new ArrayList<>();
        args.add(POST_CACHE_TTL_SECONDS);
        cachePost.forEach((filed,value) -> {
            args.add(filed);
            args.add(value);
        });
        // 执行lua脚本
        stringRedisTemplate.execute(HSET_WITH_TTL_SCRIPT,
                List.of(String.format(RedisKeyConstant.POST_CACHE_SUMMARY,barId,post.getId())),
                args.toArray());
        return CreatePostVO.builder()
                .id(post.getId())
                .build();

    }

    @Override
    public void deletePostById(String barId, String postId) {
        String userId = UserContext.getUserId();
        // 判断空值
        if(StrUtil.isBlank(barId)){
            throw new ClientException("请不要输入空值");
        }
        LambdaUpdateWrapper<PostDO> wrapper = Wrappers.lambdaUpdate(PostDO.class)
                .eq(PostDO::getUserId, userId)
                .eq(PostDO::getId, postId);

        int affected = postMapper.delete(wrapper);
        if(affected == 0){
            throw new ClientException(PostErrorCode.POST_DELETED.getMessage(),null,PostErrorCode.POST_DELETED);
        }
        // 删除缓存
        stringRedisTemplate.delete(String.format(RedisKeyConstant.POST_CACHE_SUMMARY,barId,postId));
    }

    @Override
    public void updatePostById(String barId, UpdatePostRequest request) {

        if(StrUtil.isBlank(request.getPostId())){
            throw new ClientException("请不要输入空值");
        }

        PostDO post = postMapper.selectById(request.getPostId());
        if(post == null){
            throw new ClientException(PostErrorCode.POST_NOT_FOUND.getMessage(),null,PostErrorCode.POST_NOT_FOUND);
        }

        PostDO nowPost = PostDO.builder()
                .id(request.getPostId())
                .title(request.getTitle())
                .content(request.getContent())
                .build();
        postMapper.updateById(nowPost);

        stringRedisTemplate.delete(String.format(RedisKeyConstant.POST_CACHE_SUMMARY,barId,request.getPostId()));


    }

    @Override
    public PostSummaryVO getPostSummary(String barId,String postId) {

        /*
        错误示范，entries命令不管hash是否为空都不为null
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(String.format(RedisKeyConstant.POST_CACHE_SUMMARY, barId, postId));
        if(entries != null){
            Map<String, String> postSummaryMap = entries.entrySet().stream()
                    .collect(Collectors.toMap(k -> String.valueOf(k.getKey()), v -> String.valueOf(v.getValue())));
            return BeanUtil.toBean(postSummaryMap,PostSummaryVO.class);
        }
         */
        // 1. 快路径：先查缓存，命中直接返回（不抢锁）
        Map<Object, Object> postSummary = stringRedisTemplate.opsForHash().entries(String.format(RedisKeyConstant.POST_CACHE_SUMMARY, barId, postId));
        if (!postSummary.isEmpty()) {
            return BeanUtil.toBean(MyMapUtils.mapToStingMap(postSummary), PostSummaryVO.class);
        }
        // 缓存没数据查询空缓存
        String isNUll = stringRedisTemplate.opsForValue().get(String.format(RedisKeyConstant.POST_NULL_CACHE_SUMMARY, barId, postId));
        // 空缓存不为空，直接返回错误信息
        if(isNUll != null){
            throw new ClientException(PostErrorCode.POST_NOT_FOUND.getMessage(),null,PostErrorCode.POST_NOT_FOUND);
        }

        // 使用redission分布式锁构建缓存并使用双重判定锁构建
        RLock lock = redissonClient.getLock(String.format(RedisKeyConstant.POST_SUMMARY_LOCK, barId, postId));
        try {
            if(lock.tryLock(2,TimeUnit.SECONDS)){
                Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(String.format(RedisKeyConstant.POST_CACHE_SUMMARY, barId, postId));
                if(! entries.isEmpty()){
                    Map<String, String> map = MyMapUtils.mapToStingMap(entries);
                    PostSummaryVO summaryVO = BeanUtil.toBean(map, PostSummaryVO.class);
                    return summaryVO;
                }
                String checkNullCache = stringRedisTemplate.opsForValue().get(String.format(RedisKeyConstant.POST_NULL_CACHE_SUMMARY, barId, postId));
                // 空缓存不为空，直接返回错误信息
                if(checkNullCache != null){
                    throw new ClientException(PostErrorCode.POST_NOT_FOUND.getMessage(),null,PostErrorCode.POST_NOT_FOUND);
                }

                PostDO postDO = postMapper.selectById(postId);
                // 如果数据库没有数据缓存空缓存抛异常
                if(postDO == null) {
                    stringRedisTemplate.opsForValue().set(String.format(RedisKeyConstant.POST_NULL_CACHE_SUMMARY, barId, postId),
                            "", 5, TimeUnit.MINUTES);
                    throw new ClientException(PostErrorCode.POST_NOT_FOUND.getMessage(),null,PostErrorCode.POST_NOT_FOUND);
                }
                // 存在设置缓存然后删除空缓存
                PostSummaryVO summaryVO = BeanUtil.toBean(postDO, PostSummaryVO.class);
                if( summaryVO.getContent().length() > 35){
                    summaryVO.setContent(summaryVO.getContent().substring(0,35) + "...");
                }
                // 补上未刷库的点赞/收藏增量，保证 缓存 = DB列 + 缓冲（否则缓存过期重建会丢增量）
                Object pendingLike = stringRedisTemplate.opsForHash().get(
                        String.format(RedisKeyConstant.POST_COUNT_INCR, "like_count"), postId);
                if (pendingLike != null) {
                    summaryVO.setLikeCount(summaryVO.getLikeCount() + Integer.parseInt(pendingLike.toString()));
                }
                Object pendingFavorite = stringRedisTemplate.opsForHash().get(
                        String.format(RedisKeyConstant.POST_COUNT_INCR, "favorite_count"), postId);
                if (pendingFavorite != null) {
                    summaryVO.setFavoriteCount(summaryVO.getFavoriteCount() + Integer.parseInt(pendingFavorite.toString()));
                }
                Object pendingComment = stringRedisTemplate.opsForHash().get(
                        String.format(RedisKeyConstant.POST_COUNT_INCR, "comment_count"), postId);
                if (pendingComment != null) {
                    summaryVO.setCommentCount(summaryVO.getCommentCount() + Integer.parseInt(pendingComment.toString()));
                }
                Map<String, Object> cacheMap = BeanUtil.beanToMap(summaryVO);
                Map<String, String> postCacheMap = new HashMap<>();
                for(String key : cacheMap.keySet()){
                    postCacheMap.put(key,String.valueOf(cacheMap.get(key)));
                }

                ArrayList<Object> args = new ArrayList<>();
                args.add(POST_CACHE_TTL_SECONDS);
                postCacheMap.forEach((filed,value) -> {
                    args.add(filed);
                    args.add(value);
                });
                // 执行lua脚本
                stringRedisTemplate.execute(HSET_WITH_TTL_SCRIPT,
                        List.of(String.format(RedisKeyConstant.POST_CACHE_SUMMARY,barId,postId)),
                        args.toArray());
                return summaryVO;
            }
        } catch (InterruptedException e) {
            // 恢复当前线程打断状态
            Thread.currentThread().interrupt();
            Log.error("获取锁{}时被打断",String.format(RedisKeyConstant.POST_SUMMARY_LOCK, barId, postId));
            throw new ServiceException(BaseEnums.SYSTEM_ERROR.getMessage(),e,BaseEnums.SYSTEM_ERROR);
        } finally {
            // 如果锁是当前线程获取的才释放，防止误删锁
            if(lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        // 没抢到锁兜底，查询数据库
        PostDO postDO = postMapper.selectById(postId);

        if(postDO == null) {
            throw new ClientException(PostErrorCode.POST_NOT_FOUND.getMessage(),null,PostErrorCode.POST_NOT_FOUND);
        }

        PostSummaryVO summaryVO = BeanUtil.toBean(postDO, PostSummaryVO.class);
        if( summaryVO.getContent().length() > 35){
            summaryVO.setContent(summaryVO.getContent().substring(0,35) + "...");
        }
        return summaryVO;

    }

    private Map<String,String> createCachePost(String postId, CreatePostRequest request,String barId){
        HashMap<String, String> hashMap = new HashMap<>();
        // 构建缓存
        // 现缓存id吧，我也不知道后续有没有用
        hashMap.put("id",postId);
        hashMap.put("barId",barId);
        // 标题全部缓存，因为标题是一个文章的重要信息而且我们也做过限制了（5-30）
        hashMap.put("title",request.getTitle());
        hashMap.put("content",request.getContent().length() > 35 ?
                request.getContent().substring(0,35) + "..." : request.getContent());
        hashMap.put("likeCount","0");
        hashMap.put("commentCount","0");
        hashMap.put("favoriteCount","0");
        hashMap.put("viewCount", "0");                   // 补上，避免命中时 null
        hashMap.put("coverImage", "");
        // todo 缓存封面
        return hashMap;
    }
}
