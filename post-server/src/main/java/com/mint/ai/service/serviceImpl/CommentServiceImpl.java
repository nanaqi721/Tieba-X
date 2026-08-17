package com.mint.ai.service.serviceImpl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mint.ai.common.Result;
import com.mint.ai.common.coontext.UserContext;
import com.mint.ai.common.enums.CommentErrorCode;
import com.mint.ai.common.enums.PostErrorCode;
import com.mint.ai.common.exception.ClientException;
import com.mint.ai.common.redisKey.RedisKeyConstant;
import com.mint.ai.user.api.vo.UserBaseVO;
import com.mint.ai.post.api.dto.CreateCommentRequest;
import com.mint.ai.post.api.vo.CommentFloorVO;
import com.mint.ai.post.api.vo.CreateCommentVO;
import com.mint.ai.post.api.vo.FloorPageResponseVO;
import com.mint.ai.post.api.vo.FloorVO;
import com.mint.ai.mapper.AttachmentMapper;
import com.mint.ai.mapper.CommentMapper;
import com.mint.ai.mapper.PostMapper;
import com.mint.ai.mapper.entity.AttachmentDO;
import com.mint.ai.mapper.entity.CommentDO;
import com.mint.ai.mapper.entity.PostDO;
import com.mint.ai.service.CommentService;
import com.mint.ai.user.api.clients.UserClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评论模块实现层
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {


    private final CommentMapper commentMapper;

    private final PostMapper postMapper;

    private final AttachmentMapper attachmentMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final UserClient userClient;

    private static final String COUNT_INCR = "lua/like_incr.lua";

    // lua脚本单例构建
    private static final DefaultRedisScript<Long> COUNT_INCR_SCRIPT = new DefaultRedisScript<>();

    static {
        COUNT_INCR_SCRIPT.setLocation(new ClassPathResource(COUNT_INCR));
        COUNT_INCR_SCRIPT.setResultType(Long.class);
    }

    @Override
    @Transactional
    public CreateCommentVO createComment(String postId, CreateCommentRequest request) {

        String userId = UserContext.getUserId();
        if(StrUtil.isNotBlank(request.getParentId()) && CollUtil.isNotEmpty(request.getImages())){
            throw new ClientException("回复楼中楼评论不能使用图片");
        }

        PostDO postDO = postMapper.selectById(postId);
        // 如果帖子不存在抛异常
        if(postDO == null){
            throw new ClientException(PostErrorCode.POST_NOT_FOUND.getMessage(),null,PostErrorCode.POST_NOT_FOUND);
        }

        // 构建评论请求体
        CommentDO commentDO = CommentDO.builder()
                .postId(postId)
                .userId(userId)
                .content(request.getContent())
                .likeCount(0)
                .status(0)
                .build();
        // 如果父id为空（null 或 ""），建楼；否则为楼中楼回复（不占楼层）
        if (StrUtil.isBlank(request.getParentId())) {
            // 行锁原子递增楼数并更新最后回复时间（同一 UPDATE，并发不混乱）
            postMapper.update(Wrappers.lambdaUpdate(PostDO.class)
                    .eq(PostDO::getId, postId)
                    .setSql("last_floor = last_floor + 1")
                    .set(PostDO::getLastReplyTime, LocalDateTime.now()));
            Integer lastFloor = postMapper.selectById(postId).getLastFloor();
            commentDO.setFloor(lastFloor);
            commentMapper.insert(commentDO);
            // 顶层评论支持带图
            List<String> images = request.getImages();
            if (CollUtil.isNotEmpty(images)) {
                List<AttachmentDO> attachmentDOS = buildAttachmentWithUrl(images, commentDO.getId());
                attachmentMapper.batchInsert(attachmentDOS);
            }
            // 所有 DB 操作之后写 Redis 计数，失败不滚回评论
            /**
             * Q：为什么再所有db后执行
             * A：如果先执行然后执行数据库发生错误造成数据回滚，评论数虚增加1
             */
            Long buf = incrCommentCount(postId, postDO.getBarId());
            int commentCount = postDO.getCommentCount() + (buf == null ? 0 : buf.intValue());
            return CreateCommentVO.builder().id(commentDO.getId()).floor(lastFloor).commentCount(commentCount).build();
        }

        // 楼中楼回复：不占楼层，只更新最后回复时间
        commentDO.setParentId(request.getParentId());
        commentMapper.insert(commentDO);
        postMapper.update(null, Wrappers.lambdaUpdate(PostDO.class)
                .eq(PostDO::getId, postId)
                .set(PostDO::getLastReplyTime, LocalDateTime.now()));
        Long buf = incrCommentCount(postId, postDO.getBarId());
        int commentCount = postDO.getCommentCount() + (buf == null ? 0 : buf.intValue());
        return CreateCommentVO.builder().id(commentDO.getId()).floor(0).commentCount(commentCount).build();
    }

    @Override
    @Transactional
    public void deleteComment(String commentId) {
        String userId = UserContext.getUserId();

        CommentDO commentDO = commentMapper.selectById(commentId);
        if (commentDO == null) {
            throw new ClientException(CommentErrorCode.COMMENT_NOT_FOUND.getMessage(), null, CommentErrorCode.COMMENT_NOT_FOUND);
        }
        // 权限：评论作者 或 楼主可删（帖子逻辑删除时拿不到楼主，退化为仅作者可删）
        PostDO postDO = postMapper.selectById(commentDO.getPostId());
        boolean isAuthor = userId.equals(commentDO.getUserId());
        boolean isPostOwner = postDO != null && userId.equals(postDO.getUserId());
        if (!isAuthor && !isPostOwner) {
            throw new ClientException(CommentErrorCode.COMMENT_NO_PERMISSION.getMessage(), null, CommentErrorCode.COMMENT_NO_PERMISSION);
        }
        // 级联收集评论及其楼中楼
        List<String> deleteIds = new ArrayList<>();
        collectCommentTree(commentId, deleteIds);
        commentMapper.deleteBatchIds(deleteIds);
        // 评论数按实际删除条数回减（楼中楼创建时也计数）；帖子已删时拿不到 barId，退化为仅更新缓冲
        decrCommentCount(commentDO.getPostId(), postDO == null ? null : postDO.getBarId(), deleteIds.size());
    }

    @Override
    public FloorPageResponseVO listFloors(String postId, Integer pageNum, Integer pageSize) {
        int page = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int size = pageSize == null ? 10 : pageSize;
        size = Math.min(Math.max(size, 1), 50);
        long offset = (long) (page - 1) * size;

        // 顶层楼层总数
        Long total = commentMapper.selectCount(Wrappers.lambdaQuery(CommentDO.class)
                .eq(CommentDO::getPostId, postId)
                .isNull(CommentDO::getParentId));
        // 本页顶层楼层（项目未配置分页拦截器，手动 LIMIT，与 getFeed 一致）
        List<CommentDO> topFloors = commentMapper.selectList(Wrappers.lambdaQuery(CommentDO.class)
                .eq(CommentDO::getPostId, postId)
                .isNull(CommentDO::getParentId)
                .orderByAsc(CommentDO::getFloor)
                .last("LIMIT " + offset + ", " + size));
        List<CommentFloorVO> floors = BeanUtil.copyToList(topFloors, CommentFloorVO.class);

        // 楼中楼：按 parentId 分组挂到对应顶层楼层
        if (CollUtil.isNotEmpty(floors)) {
            List<String> topIds = floors.stream().map(CommentFloorVO::getId).toList();
            List<CommentDO> children = commentMapper.selectList(Wrappers.lambdaQuery(CommentDO.class)
                    .eq(CommentDO::getPostId, postId)
                    .in(CommentDO::getParentId, topIds)
                    .orderByAsc(CommentDO::getCreateTime));
            Map<String, List<CommentFloorVO>> childrenMap = children.stream()
                    .map(c -> BeanUtil.toBean(c, CommentFloorVO.class))
                    .collect(Collectors.groupingBy(CommentFloorVO::getParentId));
            for (CommentFloorVO floor : floors) {
                floor.setChildren(childrenMap.getOrDefault(floor.getId(), List.of()));
            }
        }

        long totalCount = total == null ? 0 : total;
        long totalPages = (totalCount + size - 1) / size;

        // 聚合楼层作者昵称/头像（user-server 批量查询）
        List<FloorVO> records = new ArrayList<>(floors.size());
        if (CollUtil.isNotEmpty(floors)) {
            Set<String> userIds = new HashSet<>();
            collectFloorUserIds(floors, userIds);
            Map<String, UserBaseVO> userMap = Map.of();
            if (!userIds.isEmpty()) {
                Result<Map<String, UserBaseVO>> userResult = userClient.batchGetUsersByIds(new ArrayList<>(userIds));
                if (Result.SUCCESS_CODE.equals(userResult.getCode()) && userResult.getData() != null) {
                    userMap = userResult.getData();
                }
            }
            for (CommentFloorVO floor : floors) {
                records.add(buildFloorVO(floor, userMap));
            }
        }

        FloorPageResponseVO response = new FloorPageResponseVO();
        response.setRecords(records);
        response.setTotal(totalCount);
        response.setTotalPages(totalPages);
        response.setPageNum((long) page);
        response.setPageSize((long) size);
        return response;
    }

    /**
     * 递归收集楼层树中所有作者 userId
     */
    private void collectFloorUserIds(List<CommentFloorVO> floors, Set<String> userIds) {
        for (CommentFloorVO floor : floors) {
            if (StrUtil.isNotBlank(floor.getUserId())) {
                userIds.add(floor.getUserId());
            }
            if (CollUtil.isNotEmpty(floor.getChildren())) {
                collectFloorUserIds(floor.getChildren(), userIds);
            }
        }
    }

    /**
     * CommentFloorVO → FloorVO，填楼层作者昵称并递归挂接楼中楼
     */
    private FloorVO buildFloorVO(CommentFloorVO floor, Map<String, UserBaseVO> userMap) {
        FloorVO vo = BeanUtil.copyProperties(floor, FloorVO.class, "children");
        UserBaseVO user = userMap.get(floor.getUserId());
        vo.setNickname(user == null ? "未知用户" : user.getNickname());
        vo.setAvatarUrl(user == null ? null : user.getAvatarUrl());
        if (CollUtil.isNotEmpty(floor.getChildren())) {
            List<FloorVO> children = new ArrayList<>(floor.getChildren().size());
            for (CommentFloorVO child : floor.getChildren()) {
                children.add(buildFloorVO(child, userMap));
            }
            vo.setChildren(children);
        }
        return vo;
    }

    /**
     * 递归收集评论及其子树 id（含自身）
     */
    private void collectCommentTree(String parentId, List<String> deleteIds) {
        deleteIds.add(parentId);
        List<CommentDO> children = commentMapper.selectList(Wrappers.lambdaQuery(CommentDO.class)
                .eq(CommentDO::getParentId, parentId));
        for (CommentDO child : children) {
            collectCommentTree(child.getId(), deleteIds);
        }
    }

    /**
     * 评论计数增量写 Redis（失败不滚回评论），返回缓冲增量值（供计算最新评论数）
     */
    private Long incrCommentCount(String postId, String barId) {
        try {
            // 计数缓冲 + 摘要缓存同步更新（lua 一步完成），与点赞/收藏同脚本
            return stringRedisTemplate.execute(COUNT_INCR_SCRIPT,
                    List.of(String.format(RedisKeyConstant.POST_COUNT_INCR, "comment_count"),
                            String.format(RedisKeyConstant.POST_CACHE_SUMMARY, barId, postId)),
                    postId, "1", "commentCount");
        } catch (Exception e) {
            log.warn("评论计数增量写 Redis 失败: postId={}", postId, e);
            return null;
        }
    }

    /**
     * 评论计数回减写 Redis（失败不滚回删除）
     */
    private void decrCommentCount(String postId, String barId, int delta) {
        try {
            // 帖子已删时 barId 为 null，拿不到摘要缓存 key，退化为仅更新缓冲
            if (StrUtil.isBlank(barId)) {
                stringRedisTemplate.opsForHash()
                        .increment(String.format(RedisKeyConstant.POST_COUNT_INCR, "comment_count"), postId, -delta);
                return;
            }
            stringRedisTemplate.execute(COUNT_INCR_SCRIPT,
                    List.of(String.format(RedisKeyConstant.POST_COUNT_INCR, "comment_count"),
                            String.format(RedisKeyConstant.POST_CACHE_SUMMARY, barId, postId)),
                    postId, String.valueOf(-delta), "commentCount");
        } catch (Exception e) {
            log.warn("评论计数回减写 Redis 失败: postId={}, delta={}", postId, delta, e);
        }
    }

    private List<AttachmentDO> buildAttachmentWithUrl(List<String> urls,String commentId){
        List<AttachmentDO> list = new ArrayList<>(urls.size());
        int i = 0;
        for(String url : urls){
            AttachmentDO build = AttachmentDO.builder()
                    .id(String.valueOf(IdWorker.getId()))   // 自定义XML insert 不会自动生成雪花id
                    .bizType(2)
                    .bizId(commentId)
                    .url(url)
                    .sortOrder(++i)
                    .build();
            list.add(build);
        }
        return list;
    }
}
