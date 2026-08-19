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
import com.mint.ai.common.dto.CreateCommentRequest;
import com.mint.ai.common.vo.CreateCommentVO;
import com.mint.ai.common.vo.FloorPageResponseVO;
import com.mint.ai.common.vo.FloorVO;
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
import java.util.*;
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

        // 父评论有没有删除 父评论是不是别的帖子的 父评论是否删除了
        CommentDO parent = commentMapper.selectById(request.getParentId());
        if(parent == null || !postId.equals(parent.getPostId()) || parent.getDeleted() == 1) {
            throw new ClientException("父评论不存在");
        }
        String rootId = parent.getParentId() == null
                ? parent.getId()
                : parent.getRootId();
        // 楼中楼回复：不占楼层，只更新最后回复时间
        commentDO.setParentId(request.getParentId());
        commentDO.setRootId(rootId);
        commentMapper.insert(commentDO);
        // 按顶层楼维护回复总数，避免回复评论时顶层楼数量不准确
        if (commentMapper.incrementRootReplyCount(postId, rootId) != 1) {
            throw new ClientException("顶层评论不存在");
        }
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
        // 如果是顶楼，直接删除全部
        if(commentDO.getParentId() == null){
            int delete = commentMapper.delete(
                    Wrappers.lambdaUpdate(CommentDO.class)
                            .eq(CommentDO::getRootId, commentDO.getId()) // 所有子评论
                            .or()
                            .eq(CommentDO::getId, commentDO.getId()) // 顶层评论
            );
            decrCommentCount(commentDO.getPostId(),
                    postDO == null ? null : postDO.getBarId(), delete);
            return ;
        }
        // 如果只是顶层评论的其中一条，删除自己
        int delete = commentMapper.deleteById(commentId);
        if (delete == 1) {
            commentMapper.decrementRootReplyCount(commentDO.getPostId(), commentDO.getRootId());
            decrCommentCount(commentDO.getPostId(),
                    postDO == null ? null : postDO.getBarId(), 1);
        }
    }

    @Override
    public FloorPageResponseVO pageQueryComments(String postId, Integer pageNum, Integer pageSize) {
        PostDO postDO = postMapper.selectById(postId);
        if(postDO == null){
            throw new ClientException(PostErrorCode.POST_NOT_FOUND.getMessage(),null,PostErrorCode.POST_NOT_FOUND);
        }
        int page = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int size = pageSize == null ? 10 : pageSize;
        size = Math.min(Math.max(size, 1), 50);
        long offset = (long) (page - 1) * size;

        Long total = commentMapper.countTopFloors(postId);
        List<FloorVO> commentFloorList = commentMapper.selectFloorsWithReplies(
                postId, offset, size, 3);
        if (commentFloorList == null) {
            commentFloorList = new ArrayList<>();
        }

        List<String> floorIds = commentFloorList.stream()
                .map(FloorVO::getId)
                .toList();
        List<AttachmentDO> attachments = floorIds.isEmpty()
                ? List.of()
                : attachmentMapper.selectList(
                        Wrappers.lambdaQuery(AttachmentDO.class)
                                .eq(AttachmentDO::getBizType, 2)
                                .in(AttachmentDO::getBizId, floorIds)
                                .orderByAsc(AttachmentDO::getSortOrder)
                );

        Map<String, List<String>> imageMap = attachments.stream()
                .collect(Collectors.groupingBy(
                        AttachmentDO::getBizId,
                        Collectors.mapping(
                                AttachmentDO::getUrl,
                                Collectors.toList()
                        )
                ));
        for (FloorVO floor : commentFloorList) {
            floor.setImages(
                    imageMap.getOrDefault(floor.getId(), List.of())
            );
        }

        Set<String> userIds = new HashSet<>();
        for (FloorVO floor : commentFloorList) {
            collectFloorUserIds(floor, userIds);
            if (floor.getChildren() == null) {
                floor.setChildren(new ArrayList<>());
            }
        }

        Map<String, UserBaseVO> userData = Map.of();
        if (!userIds.isEmpty()) {
            Result<Map<String, UserBaseVO>> userClientResult =
                    userClient.batchGetUsersByIds(new ArrayList<>(userIds));
            if (Result.SUCCESS_CODE.equals(userClientResult.getCode())
                    && userClientResult.getData() != null) {
                userData = userClientResult.getData();
            }
        }
        fillFloorUsers(commentFloorList, userData);

        long totalCount = total == null ? 0L : total;
        long totalPages = (totalCount + size - 1) / size;
        return FloorPageResponseVO.builder()
                .records(commentFloorList)
                .pageSize((long) size)
                .pageNum((long) page)
                .total(totalCount)
                .totalPages(totalPages)
                .build();
    }

    private void collectFloorUserIds(FloorVO floor, Set<String> userIds) {
        if (StrUtil.isNotBlank(floor.getUserId())) {
            userIds.add(floor.getUserId());
        }
        if (CollUtil.isNotEmpty(floor.getChildren())) {
            for (FloorVO child : floor.getChildren()) {
                collectFloorUserIds(child, userIds);
            }
        }
    }

    private void fillFloorUsers(List<FloorVO> floors,
                                Map<String, UserBaseVO> userData) {
        for (FloorVO floor : floors) {
            UserBaseVO user = userData.get(floor.getUserId());
            floor.setNickname(user == null ? "未知用户" : user.getNickname());
            floor.setAvatarUrl(user == null ? null : user.getAvatarUrl());
            if (CollUtil.isNotEmpty(floor.getChildren())) {
                fillFloorUsers(floor.getChildren(), userData);
            }
        }
    }

    @Override
    public FloorPageResponseVO pageQueryReplies(String postId, String rootId,
                                                Integer pageNum, Integer pageSize) {
        CommentDO root = commentMapper.selectById(rootId);
        if (root == null
                || !postId.equals(root.getPostId())
                || root.getParentId() != null) {
            throw new ClientException("楼层不存在");
        }
        int page = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int size = pageSize == null ? 20 : Math.min(Math.max(pageSize, 1), 50);
        long offset = (long) (page - 1) * size;

        Long total = commentMapper.selectCount(
                Wrappers.lambdaQuery(CommentDO.class)
                        .eq(CommentDO::getPostId, postId)
                        .eq(CommentDO::getRootId, rootId)
                        .isNotNull(CommentDO::getParentId)
        );
        List<CommentDO> replies = commentMapper.selectList(
                Wrappers.lambdaQuery(CommentDO.class)
                        .eq(CommentDO::getPostId, postId)
                        .eq(CommentDO::getRootId, rootId)
                        .isNotNull(CommentDO::getParentId)
                        .orderByAsc(CommentDO::getCreateTime)
                        .orderByAsc(CommentDO::getId)
                        .last("LIMIT " + offset + ", " + size)
        );

        List<FloorVO> records = BeanUtil.copyToList(replies, FloorVO.class);
        Set<String> userIds = new HashSet<>();
        for (FloorVO reply : records) {
            if (StrUtil.isNotBlank(reply.getUserId())) {
                userIds.add(reply.getUserId());
            }
            reply.setChildren(new ArrayList<>());
        }
        Map<String, UserBaseVO> userData = Map.of();
        if (!userIds.isEmpty()) {
            Result<Map<String, UserBaseVO>> userResult =
                    userClient.batchGetUsersByIds(new ArrayList<>(userIds));
            if (Result.SUCCESS_CODE.equals(userResult.getCode())
                    && userResult.getData() != null) {
                userData = userResult.getData();
            }
        }
        fillFloorUsers(records, userData);
        long totalCount = total == null ? 0L : total;
        return FloorPageResponseVO.builder()
                .records(records)
                .total(totalCount)
                .totalPages((totalCount + size - 1) / size)
                .pageNum((long) page)
                .pageSize((long) size)
                .build();
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
