package com.mint.ai.service.serviceImpl;
import cn.hutool.core.util.StrUtil;
import com.mint.ai.common.enums.PostErrorCode;
import com.mint.ai.common.exception.ClientException;
import com.mint.ai.common.redisKey.RedisKeyConstant;
import com.mint.ai.common.vo.CreatePostVO;
import com.mint.ai.common.dto.CreatePostRequest;
import com.mint.ai.common.dto.UpdatePostRequest;
import com.mint.ai.mapper.PostMapper;
import com.mint.ai.mapper.entiy.PostDO;
import com.mint.ai.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 帖子控制层
 */
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;

    private final StringRedisTemplate stringRedisTemplate;

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

        PostDO post = PostDO.builder()
                .bar_id("1001")
                .user_id("1001")
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
                List.of(String.format(RedisKeyConstant.POST_CACHE_DETAIL,barId,post.getId())),
                args.toArray());
        return CreatePostVO.builder()
                .id(post.getId())
                .build();

    }

    @Override
    public void deletePostById(String barId, String postId) {
        // 判断空值
        if(StrUtil.isBlank(barId)){
            throw new ClientException("请不要输入空值");
        }
        int affected = postMapper.deleteById(postId);
        if(affected == 0){
            throw new ClientException(PostErrorCode.POST_DELETED.getMessage(),null,PostErrorCode.POST_DELETED);
        }
        // 删除缓存
        stringRedisTemplate.delete(String.format(RedisKeyConstant.POST_CACHE_DETAIL,barId,postId));
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

        stringRedisTemplate.delete(String.format(RedisKeyConstant.POST_CACHE_DETAIL,barId,request.getPostId()));


    }

    private Map<String,String> createCachePost(String postId, CreatePostRequest request,String barId){
        HashMap<String, String> hashMap = new HashMap<>();
        // 构建缓存
        // 现缓存id吧，我也不知道后续有没有用
        hashMap.put("id",postId);
        hashMap.put("barID",barId);
        // 标题全部缓存，因为标题是一个文章的重要信息而且我们也做过限制了（5-30）
        hashMap.put("title",request.getTitle());
        hashMap.put("content",request.getContent().length() > 35 ?
                request.getContent().substring(0,35) + "..." : request.getContent());
        hashMap.put("likeCount","0");
        hashMap.put("commentCount","0");
        hashMap.put("favoriteCount","0");
        // todo 缓存封面
        return hashMap;
    }
}
