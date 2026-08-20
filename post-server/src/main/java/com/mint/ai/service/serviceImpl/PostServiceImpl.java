package com.mint.ai.service.serviceImpl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.esotericsoftware.minlog.Log;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mint.ai.bar.api.clients.BarClient;
import com.mint.ai.bar.api.vo.BarBaseVO;
import com.mint.ai.common.Result;
import com.mint.ai.common.coontext.UserContext;
import com.mint.ai.common.cursor.BarPostFeedCursor;
import com.mint.ai.common.cursor.PostHomeFeedCursor;
import com.mint.ai.common.cursor.PostSearchCursor;
import com.mint.ai.common.dto.PostFeedInBarRequest;
import com.mint.ai.common.enums.BaseEnums;
import com.mint.ai.common.enums.PostErrorCode;
import com.mint.ai.common.exception.ClientException;
import com.mint.ai.common.exception.ServiceException;
import com.mint.ai.common.redisKey.RedisConstantKey;
import com.mint.ai.common.dto.UpdatePostRequest;
import com.mint.ai.common.vo.PostFeedInBarVO;
import com.mint.ai.common.vo.*;
import com.mint.ai.user.api.vo.UserBaseVO;
import com.mint.ai.mapper.PostMapper;
import com.mint.ai.common.dto.CreatePostRequest;
import com.mint.ai.mapper.entity.PostDO;
import com.mint.ai.mq.enums.UserContentType;
import com.mint.ai.mq.producer.UserContentChangedProducer;
import com.mint.ai.service.PostService;
import com.mint.ai.user.api.clients.UserClient;
import com.mint.ai.utils.MyMapUtils;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 帖子控制层
 */
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final ObjectMapper objectMapper;

    private final PostMapper postMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final RedissonClient redissonClient;

    private final BarClient barClient;

    private final UserClient userClient;

    private final UserContentChangedProducer userContentChangedProducer;

    // lua脚本路径
    private static final String HSET_WITH_TTL = "lua/hset_with_ttl.lua";

    private static final String BAR_POST_COUNT_INCR = "lua/bar_post_count_incr.lua";

    // 帖子缓存过期时间
    private static final String POST_CACHE_TTL_SECONDS = "3600";

    // 自动初始化脚本
    private static final DefaultRedisScript<Long> HSET_WITH_TTL_SCRIPT = new DefaultRedisScript<>();

    private static final DefaultRedisScript<Long> BAR_POST_COUNT_INCR_SCRIPT = new DefaultRedisScript<>();

    static {
        HSET_WITH_TTL_SCRIPT.setLocation(new ClassPathResource(HSET_WITH_TTL));
        HSET_WITH_TTL_SCRIPT.setResultType(Long.class);

        BAR_POST_COUNT_INCR_SCRIPT.setLocation(new ClassPathResource(BAR_POST_COUNT_INCR));
        BAR_POST_COUNT_INCR_SCRIPT.setResultType(Long.class);
    }
    @Override
    @Transactional
    public PostCreateVO createPost(CreatePostRequest request) {

        // 登录已由 LoginInterceptor 兜底，此处直接取用户 id
        String userId = UserContext.getUserId();
        PostDO post = PostDO.builder()
                .barId(request.getBarId())
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
        Map<String, String> cachePost = createCachePost(post.getId(), request,request.getBarId());
        ArrayList<Object> args = new ArrayList<>();
        args.add(POST_CACHE_TTL_SECONDS);
        cachePost.forEach((filed,value) -> {
            args.add(filed);
            args.add(value);
        });
        // 执行lua脚本
        stringRedisTemplate.execute(HSET_WITH_TTL_SCRIPT,
                List.of(String.format(RedisConstantKey.POST_CACHE_SUMMARY,request.getBarId(),post.getId())),
                args.toArray());

        // 清除历史空缓存，避免"帖子已创建但空缓存未过期"导致查询误判不存在
        stringRedisTemplate.delete(String.format(RedisConstantKey.POST_NULL_CACHE_SUMMARY, request.getBarId(), post.getId()));
        // 帖子数增量入 buffer + 吧详情缓存同步，由 bar-server 定时任务刷入 bar.post_count
        stringRedisTemplate.execute(BAR_POST_COUNT_INCR_SCRIPT,
                List.of(String.format(RedisConstantKey.BAR_COUNT_INCR, "post_count"),
                        String.format(RedisConstantKey.BAR_DETAIL_CACHE, request.getBarId())),
                request.getBarId(), "1", "postCount");
        userContentChangedProducer.send(
                userId, UserContentType.POST, 1, "用户创建帖子", post.getId());
        return PostCreateVO.builder()
                .id(post.getId())
                .build();

    }

    @Override
    @Transactional
    public void deletePostById(String postId) {
        String userId = UserContext.getUserId();
        LambdaUpdateWrapper<PostDO> wrapper = Wrappers.lambdaUpdate(PostDO.class)
                .eq(PostDO::getUserId, userId)
                .eq(PostDO::getId, postId);
        PostDO postDO = postMapper.selectById(postId);
        int affected = postMapper.delete(wrapper);
        if(affected == 0){
            throw new ClientException(PostErrorCode.POST_DELETED.getMessage(),null,PostErrorCode.POST_DELETED);
        }
        // 删除缓存
        stringRedisTemplate.delete(String.format(RedisConstantKey.POST_CACHE_SUMMARY,postDO.getId(),postId));
        // 帖子数减量入 buffer + 吧详情缓存同步，由 bar-server 定时任务刷入 bar.post_count
        stringRedisTemplate.execute(BAR_POST_COUNT_INCR_SCRIPT,
                List.of(String.format(RedisConstantKey.BAR_COUNT_INCR, "post_count"),
                        String.format(RedisConstantKey.BAR_DETAIL_CACHE, postDO.getId())),
                postDO.getId(), "-1", "postCount");
        userContentChangedProducer.send(
                postDO.getUserId(), UserContentType.POST, -1, "用户删除帖子", postDO.getId());
    }

    @Override
    public void updatePostById(UpdatePostRequest request) {

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
        if (postMapper.updateById(nowPost) != 1) {
            throw new ClientException(PostErrorCode.POST_UPDATE_ERROR.getMessage(),
                    null, PostErrorCode.POST_UPDATE_ERROR);
        }

        stringRedisTemplate.delete(String.format(RedisConstantKey.POST_CACHE_SUMMARY,post.getBarId(),request.getPostId()));


    }

    @Override
    public PostSummaryVO getPostSummary(String postId) {

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
        Map<Object, Object> postSummary = stringRedisTemplate.opsForHash().entries(String.format(RedisConstantKey.POST_CACHE_SUMMARY,postId));
        if (!postSummary.isEmpty()) {
            PostSummaryVO postSummaryVO = BeanUtil.toBean(MyMapUtils.mapToStingMap(postSummary), PostSummaryVO.class);
            stringRedisTemplate.execute(
                    BAR_POST_COUNT_INCR_SCRIPT,
                    List.of(
                            String.format(RedisConstantKey.POST_COUNT_INCR, "view_count"),
                            String.format(RedisConstantKey.POST_CACHE_SUMMARY,postSummaryVO.getBarId(), postId)
                    ),
                    postId,
                    "1",
                    "viewCount"
            );
            return postSummaryVO;
        }
        // 缓存没数据查询空缓存
        String isNUll = stringRedisTemplate.opsForValue().get(String.format(RedisConstantKey.POST_NULL_CACHE_SUMMARY,postId));
        // 空缓存不为空，直接返回错误信息
        if(isNUll != null){
            throw new ClientException(PostErrorCode.POST_NOT_FOUND.getMessage(),null,PostErrorCode.POST_NOT_FOUND);
        }

        // 使用redission分布式锁构建缓存并使用双重判定锁构建
        RLock lock = redissonClient.getLock(String.format(RedisConstantKey.POST_SUMMARY_LOCK,postId));
        try {
            if(lock.tryLock(2,TimeUnit.SECONDS)){
                Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(String.format(RedisConstantKey.POST_CACHE_SUMMARY,postId));
                if(! entries.isEmpty()){
                    Map<String, String> map = MyMapUtils.mapToStingMap(entries);
                    PostSummaryVO summaryVO = BeanUtil.toBean(map, PostSummaryVO.class);
                    return summaryVO;
                }
                String checkNullCache = stringRedisTemplate.opsForValue().get(String.format(RedisConstantKey.POST_NULL_CACHE_SUMMARY,postId));
                // 空缓存不为空，直接返回错误信息
                if(checkNullCache != null){
                    throw new ClientException(PostErrorCode.POST_NOT_FOUND.getMessage(),null,PostErrorCode.POST_NOT_FOUND);
                }

                PostDO postDO = postMapper.selectById(postId);
                // 如果数据库没有数据缓存空缓存抛异常
                if(postDO == null) {
                    stringRedisTemplate.opsForValue().set(String.format(RedisConstantKey.POST_NULL_CACHE_SUMMARY,postId),
                            "", 5, TimeUnit.MINUTES);
                    throw new ClientException(PostErrorCode.POST_NOT_FOUND.getMessage(),null,PostErrorCode.POST_NOT_FOUND);
                }
                // 构建摘要 VO（含内容截断 + 补未刷库增量）
                PostSummaryVO summaryVO = buildSummaryVO(postDO);

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
                        List.of(String.format(RedisConstantKey.POST_CACHE_SUMMARY,summaryVO.getBarId(),postId)),
                        args.toArray());
                // 删除空缓存，避免"帖子已存在但空缓存未过期"导致查询误判不存在
                stringRedisTemplate.delete(String.format(RedisConstantKey.POST_NULL_CACHE_SUMMARY,postId));
                stringRedisTemplate.execute(
                        BAR_POST_COUNT_INCR_SCRIPT,
                        List.of(
                                String.format(RedisConstantKey.POST_COUNT_INCR, "view_count"),
                                String.format(RedisConstantKey.POST_CACHE_SUMMARY, summaryVO.getBarId(), postId)
                        ),
                        postId,
                        "1",
                        "viewCount"
                );
                return summaryVO;
            }
        } catch (InterruptedException e) {
            // 恢复当前线程打断状态
            Thread.currentThread().interrupt();
            Log.error("获取锁{}时被打断",String.format(RedisConstantKey.POST_SUMMARY_LOCK,postId));
            throw new ServiceException(BaseEnums.SYSTEM_ERROR.getMessage(),e,BaseEnums.SYSTEM_ERROR);
        } finally {
            // 如果锁是当前线程获取的才释放，防止误删锁
            if(lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        stringRedisTemplate.execute(
                BAR_POST_COUNT_INCR_SCRIPT,
                List.of(
                        String.format(RedisConstantKey.POST_COUNT_INCR, "view_count"),
                        String.format(RedisConstantKey.POST_CACHE_SUMMARY,postId)
                ),
                postId,
                "1",
                "viewCount"
        );
        // 没抢到锁兜底，查询数据库
        PostDO postDO = postMapper.selectById(postId);

        if(postDO == null) {
            throw new ClientException(PostErrorCode.POST_NOT_FOUND.getMessage(),null,PostErrorCode.POST_NOT_FOUND);
        }

        // 兜底同样补上未刷库增量，与获锁路径行为保持一致
        return buildSummaryVO(postDO);

    }

    @Override
    public PostFeedItemVO homePostsFeed(String cursor, Integer pageSize) {
        // 首页不传 cursor → null;非法游标兜底为业务异常
        PostHomeFeedCursor pageCursor = null;
        if (StrUtil.isNotBlank(cursor)) {
            try {
                pageCursor = JSONUtil.toBean(cursor, PostHomeFeedCursor.class);
            } catch (Exception e) {
                throw new ClientException("游标无效");
            }
        }
        // clamp pageSize 到 [1,10]，默认 10
        int size = pageSize == null ? 10 : Math.max(1, Math.min(10, pageSize));

        // 多取一行判断是否还有下一页
        List<PostHomePageVO> list = postMapper.postHomePage(pageCursor, size + 1);
        boolean hasMore = list.size() > size;
        if (hasMore) {
            // 截断多取的那条，nextCursor 取应返回的最后一条（hotScore 由 SQL 计算列带出，不回表）
            list = new ArrayList<>(list.subList(0, size));
        }
        String nextCursor = null;
        if (hasMore) {
            PostHomePageVO last = list.get(list.size() - 1);
            PostHomeFeedCursor next = PostHomeFeedCursor.builder()
                    .postId(last.getPostId())
                    .score(last.getHotScore())
                    .build();
            nextCursor = JSONUtil.toJsonStr(next);
        }

        // 聚合吧信息（bar-server 批量查询）
        List<PostCardVO> records = new ArrayList<>(list.size());
        if (!list.isEmpty()) {
            List<String> barIds = list.stream()
                    .map(PostHomePageVO::getBarId)
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .toList();
            Map<String, BarBaseVO> barMap = Map.of();
            if (!barIds.isEmpty()) {
                Result<Map<String, BarBaseVO>> barResult = barClient.queryBarList(barIds);
                if (Result.SUCCESS_CODE.equals(barResult.getCode()) && barResult.getData() != null) {
                    barMap = barResult.getData();
                }
            }
            for (PostHomePageVO post : list) {
                PostCardVO vo = BeanUtil.copyProperties(post, PostCardVO.class);
                BarBaseVO bar = barMap.get(post.getBarId());
                if (bar == null) {
                    vo.setBarName("未知吧");
                    vo.setBarPostCount(0);
                    vo.setBarFollowerCount(0);
                } else {
                    vo.setBarName(bar.getName());
                    vo.setBarAvatarUrl(bar.getAvatarUrl());
                    vo.setBarPostCount(bar.getPostCount() == null ? 0 : bar.getPostCount());
                    vo.setBarFollowerCount(bar.getFollowerCount() == null ? 0 : bar.getFollowerCount());
                }
                records.add(vo);
            }
        }

        return new PostFeedItemVO().setRecords(records)
                .setNextCursor(nextCursor)
                .setHasMore(hasMore);
    }

    @Override
    public PostFeedInBarVO getPostFeedInBar(PostFeedInBarRequest request) {

        // 校验参数
        String orderBy = request.getOrderBy();
        if(orderBy != null && ! ("hot".equals(orderBy) || "createTime".equals(orderBy))){
            throw new ClientException("请输入正确的排序字段");
        }

        Integer pageSize = request.getPageSize() == null ? 10 : Math.max(1, Math.min(10, request.getPageSize()));

        // 先校验吧存在：查询失败（吧不存在等）→ 直接抛异常，不处理
        Result<BarBaseVO> result = barClient.queryBar(request.getBarId());
        if (!Result.SUCCESS_CODE.equals(result.getCode())) {
            throw new ClientException(result.getMessage()); // 透传下游错误信息，如"吧不存在"
        }
        BarPostFeedCursor cursor = request.getCursor();
        List<BarPostCardVO> postFeedInBar = postMapper.getPostFeedInBar(request.getBarId(), orderBy, cursor, pageSize + 1);
        Boolean hasNext = Boolean.FALSE;
        BarPostFeedCursor c = new BarPostFeedCursor();
        if(!postFeedInBar.isEmpty() && postFeedInBar.size() > pageSize){
            hasNext = Boolean.TRUE;
            postFeedInBar = postFeedInBar.subList(0,postFeedInBar.size() -1);
            BarPostCardVO postCardVO = postFeedInBar.get(postFeedInBar.size() - 1);
            c.setPostId(postCardVO.getPostId());
            c.setScore(postCardVO.getHotScore());
            c.setCreateTime(postCardVO.getCreateTime());
        }
        List<String> userIds = postFeedInBar.stream()
                .map(BarPostCardVO::getUserId)
                .collect(Collectors.toList());
        Map<String,UserBaseVO> userMap = Map.of();
        if(!userIds.isEmpty()){
            Result<Map<String, UserBaseVO>> userClientResult = userClient.batchGetUsersByIds(userIds);
            if(Result.SUCCESS_CODE.equals(userClientResult.getCode()) && userClientResult.getData() != null){
                userMap = userClientResult.getData();
            }
            for (BarPostCardVO barPostCardVO :postFeedInBar){
                UserBaseVO userBaseVO = userMap.get(barPostCardVO.getUserId());
                if(userBaseVO != null){
                    barPostCardVO.setAvatarUrl(userBaseVO.getAvatarUrl());
                    barPostCardVO.setNickName(userBaseVO.getNickname());
                } else {
                    barPostCardVO.setAvatarUrl("");
                    barPostCardVO.setNickName("未知");
                }
            }
        }
        return PostFeedInBarVO.builder()
                .hasNext(hasNext)
                .cursor(c)
                .data(postFeedInBar)
                .build();
    }

    @Override
    public PostSearchResultVO searchPosts(String keyword, String cursor, Integer pageSize) {
        if (keyword == null) {
            throw new ClientException("搜索词不能为空");
        }
        String normalizedKeyword = keyword.strip();
        if (normalizedKeyword.isEmpty()) {
            throw new ClientException("搜索词不能为空");
        }
        if (normalizedKeyword.length() > 20) {
            throw new ClientException("搜索词长度不能超过20个字符");
        }
        if (pageSize != null && (pageSize < 1 || pageSize > 10)) {
            throw new ClientException("pageSize必须在1到10之间");
        }
        int size = pageSize == null ? 10 : pageSize;
        String keywordDigest = DigestUtil.sha256Hex(normalizedKeyword.toLowerCase(Locale.ROOT));
        PostSearchCursor pageCursor = decodeSearchCursor(cursor, keywordDigest);
        String escapedKeyword = normalizedKeyword
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        List<PostSearchItemVO> records = postMapper.searchPosts(escapedKeyword, pageCursor, size + 1);

        boolean hasMore = records.size() > size;
        if (hasMore) {
            records = new ArrayList<>(records.subList(0, size));
        }

        List<String> barIds = records.stream()
                .map(PostSearchItemVO::getBarId)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
        Map<String, BarBaseVO> barMap = Map.of();
        if (!barIds.isEmpty()) {
            try {
                Result<Map<String, BarBaseVO>> barResult = barClient.queryBarList(barIds);
                if (Result.SUCCESS_CODE.equals(barResult.getCode()) && barResult.getData() != null) {
                    barMap = barResult.getData();
                }
            } catch (RuntimeException ignored) {
                barMap = Map.of();
            }
        }
        for (PostSearchItemVO record : records) {
            if (record.getContent() != null && record.getContent().length() > 30) {
                record.setContent(record.getContent().substring(0, 30) + "...");
            }
            BarBaseVO bar = barMap.get(record.getBarId());
            if (bar == null) {
                record.setBarName("未知吧");
                record.setBarAvatarUrl("");
            } else {
                record.setBarName(bar.getName());
                record.setBarAvatarUrl(bar.getAvatarUrl());
            }
        }

        String nextCursor = null;
        if (hasMore) {
            PostSearchItemVO last = records.get(records.size() - 1);
            nextCursor = encodeSearchCursor(PostSearchCursor.builder()
                    .postId(last.getPostId())
                    .createTime(last.getCreateTime())
                    .keywordDigest(keywordDigest)
                    .build());
        }

        return PostSearchResultVO.builder()
                .records(records)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }

    private PostSearchCursor decodeSearchCursor(String cursor, String keywordDigest) {
        if (StrUtil.isBlank(cursor)) {
            return null;
        }
        try {
            byte[] json = Base64.getUrlDecoder().decode(cursor);
            PostSearchCursor pageCursor = objectMapper.readValue(json, PostSearchCursor.class);
            if (StrUtil.isBlank(pageCursor.getPostId())
                    || pageCursor.getCreateTime() == null
                    || StrUtil.isBlank(pageCursor.getKeywordDigest())) {
                throw new ClientException("游标无效");
            }
            if (!keywordDigest.equals(pageCursor.getKeywordDigest())) {
                throw new ClientException("游标与搜索词不匹配");
            }
            return pageCursor;
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            throw new ClientException("游标无效");
        }
    }

    private String encodeSearchCursor(PostSearchCursor cursor) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(cursor);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception e) {
            throw new ServiceException("生成搜索游标失败", e);
        }
    }

    /**
     * 构建帖子摘要 VO：实体转 VO + 内容不截断 + 补上未刷库的增量
     */
    private PostSummaryVO buildSummaryVO(PostDO postDO) {
        PostSummaryVO summaryVO = BeanUtil.toBean(postDO, PostSummaryVO.class);
        // 补上未刷库的点赞/收藏/评论/浏览量增量，保证 缓存/兜底 = DB列 + 缓冲
        summaryVO.setLikeCount(addPendingCount(postDO.getId(), "like_count", summaryVO.getLikeCount()));
        summaryVO.setFavoriteCount(addPendingCount(postDO.getId(), "favorite_count", summaryVO.getFavoriteCount()));
        summaryVO.setCommentCount(addPendingCount(postDO.getId(), "comment_count", summaryVO.getCommentCount()));
        summaryVO.setViewCount(addPendingCount(postDO.getId(), "view_count", summaryVO.getViewCount()));
        return summaryVO;
    }

    /**
     * 从增量缓冲 hash 中取该帖的未刷库增量并叠加到基数上
     */
    private Integer addPendingCount(String postId, String metric, Integer base) {
        Object pending = stringRedisTemplate.opsForHash().get(
                String.format(RedisConstantKey.POST_COUNT_INCR, metric), postId);
        return pending == null ? base : base + Integer.parseInt(pending.toString());
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
