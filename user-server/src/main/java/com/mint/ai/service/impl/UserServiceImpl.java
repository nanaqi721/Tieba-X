package com.mint.ai.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mint.ai.bar.api.clients.BarClient;
import com.mint.ai.bar.api.dto.CreateBarRequest;
import com.mint.ai.bar.api.vo.BarDetailVO;
import com.mint.ai.common.Result;
import com.mint.ai.common.dto.CreateUserRequest;
import com.mint.ai.common.dto.LoginRequest;
import com.mint.ai.common.enums.BaseEnums;
import com.mint.ai.common.enums.UserErrorCode;
import com.mint.ai.common.exception.ClientException;
import com.mint.ai.common.exception.ServiceException;
import com.mint.ai.mapper.UserMapper;
import com.mint.ai.mapper.entity.UserDO;
import com.mint.ai.post.api.clients.PostClient;
import com.mint.ai.post.api.dto.CreateCommentRequest;
import com.mint.ai.post.api.dto.CreatePostRequest;
import com.mint.ai.post.api.vo.CommentFloorVO;
import com.mint.ai.post.api.vo.CreateCommentVO;
import com.mint.ai.post.api.vo.CreatePostVO;
import com.mint.ai.post.api.vo.FloorPageVO;
import com.mint.ai.post.api.vo.PostDetailVO;
import com.mint.ai.post.api.vo.PostHomePageVO;
import com.mint.ai.post.api.vo.PostHomePageWithCursor;
import com.mint.ai.post.api.vo.PostSummaryVO;
import com.mint.ai.service.UserService;
import com.mint.ai.common.vo.FeedPageVO;
import com.mint.ai.common.vo.FeedPostVO;
import com.mint.ai.common.vo.FloorPageResponseVO;
import com.mint.ai.common.vo.FloorVO;
import com.mint.ai.common.vo.PostDetailPageVO;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map;

@Service
@RequiredArgsConstructor

public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    private final PostClient postClient;

    private final BarClient barClient;
    @Override
    public String register(CreateUserRequest request, String deviceType) {
        LambdaQueryWrapper<UserDO> wrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, request.getUsername());
        // 查询该用户名是否被占用
        Long count = userMapper.selectCount(wrapper);
        if(count != 0){
            throw new ClientException(UserErrorCode.USERNAME_EXISTS.getMessage(),null,UserErrorCode.USERNAME_EXISTS);
        }

        // 发生并发冲突可能两个用户都查询到没有注册该用户名，然后进行插入，那么只能有一个插入成功然后另一个抛出错误catch捕获
        /**
         * Q：为什么不选用redis分布式锁呢
         * A：因为注册用户发生频率低不需要
         */
        UserDO userDO = BeanUtil.toBean(request, UserDO.class);
        // 将密码转化为hash值安全存储
        userDO.setPasswordHash(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));

        // 如果没有传nickName那么我们就将用户名 + 8位随机数当作nickName
        String nickName = StrUtil.isBlank(request.getNickname())
                ? request.getUsername() + RandomUtil.randomNumbers(8)
                : request.getNickname();

        userDO.setNickname(nickName);

        try {
            userMapper.insert(userDO);
        } catch (DuplicateKeyException e) {
            throw new ClientException(UserErrorCode.USERNAME_EXISTS.getMessage(),null,UserErrorCode.USERNAME_EXISTS);
        }

        /**
         * Q：为什么注册完直接登录返回 token
         * A：注册即登录，前端拿 token 即可访问后续需要鉴权的接口
         */
        StpUtil.login(userDO.getId(), deviceType);
        return StpUtil.getTokenValue();

    }

    @Override
    public String login(LoginRequest request, String deviceType) {
        LambdaQueryWrapper<UserDO> wrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, request.getUsername());
        UserDO user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new ClientException(UserErrorCode.LOGIN_ERROR.getMessage(), null, UserErrorCode.LOGIN_ERROR);
        }
        // 封禁校验（status 取值需与数据库约定一致：1 表示禁用）
        if (Integer.valueOf(1).equals(user.getStatus())) {
            throw new ClientException(UserErrorCode.USER_BANNED.getMessage(), null, UserErrorCode.USER_BANNED);
        }
        // 密码校验
        if (!BCrypt.checkpw(request.getPassword(), user.getPasswordHash())) {
            throw new ClientException(UserErrorCode.LOGIN_ERROR.getMessage(), null, UserErrorCode.LOGIN_ERROR);
        }
        StpUtil.login(user.getId(), deviceType);
        return StpUtil.getTokenValue();
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public PostSummaryVO getPostSummary(String barId,String postId) {
        Result<PostSummaryVO> result;
        try {
            result = postClient.getPostSummary(barId, postId);
        } catch (FeignException e) {
            throw new ServiceException(BaseEnums.THIRD_PARTY_ERROR.getMessage(), e, BaseEnums.THIRD_PARTY_ERROR);
        }
        if (!Result.SUCCESS_CODE.equals(result.getCode())) {   // SUCCESS_CODE = "200"
            throw new ServiceException(result.getMessage(), null, BaseEnums.SYSTEM_ERROR);
        }
        return result.getData();
    }

    @Override
    public FeedPageVO getFeed(String cursor, Integer pageSize) {
        // 1. 拉一页帖子（post-server 滑动查询）
        Result<PostHomePageWithCursor> result;
        try {
            result = postClient.postHomePage(cursor, pageSize);
        } catch (FeignException e) {
            throw new ServiceException(BaseEnums.THIRD_PARTY_ERROR.getMessage(), e, BaseEnums.THIRD_PARTY_ERROR);
        }
        if (!Result.SUCCESS_CODE.equals(result.getCode())) {
            throw new ServiceException(result.getMessage(), null, BaseEnums.SYSTEM_ERROR);
        }
        PostHomePageWithCursor data = result.getData();
        List<PostHomePageVO> posts = data.getPostHomePageVOS() == null ? List.of() : data.getPostHomePageVOS();

        List<FeedPostVO> records = new ArrayList<>(posts.size());
        if (!posts.isEmpty()) {
            // 2. 收集去重 barId → 批量查吧详情
            List<String> barIds = posts.stream()
                    .map(PostHomePageVO::getBarId)
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .toList();
            Map<String, BarDetailVO> barMap = Map.of();
            if (!barIds.isEmpty()) {
                Result<Map<String, BarDetailVO>> barResult;
                try {
                    barResult = barClient.queryBarList(barIds);
                } catch (FeignException e) {
                    throw new ServiceException(BaseEnums.THIRD_PARTY_ERROR.getMessage(), e, BaseEnums.THIRD_PARTY_ERROR);
                }
                if (Result.SUCCESS_CODE.equals(barResult.getCode()) && barResult.getData() != null) {
                    barMap = barResult.getData();
                }
            }
            // 3. 逐条组装卡片
            for (PostHomePageVO post : posts) {
                FeedPostVO vo = BeanUtil.copyProperties(post, FeedPostVO.class);
                BarDetailVO bar = barMap.get(post.getBarId());
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
        // 4. 游标/hasMore 原样透传
        return new FeedPageVO().setRecords(records)
                .setNextCursor(data.getNextCursor())
                .setHasMore(data.getHasMore());
    }

    @Override
    public PostDetailPageVO getPostDetail(String postId) {
        // 1. 拉详情（content 全量，不含楼层）
        Result<PostDetailVO> result;
        try {
            result = postClient.getPostDetail(postId);
        } catch (FeignException e) {
            throw new ServiceException(BaseEnums.THIRD_PARTY_ERROR.getMessage(), e, BaseEnums.THIRD_PARTY_ERROR);
        }
        if (!Result.SUCCESS_CODE.equals(result.getCode())) {
            throw new ServiceException(result.getMessage(), null, BaseEnums.SYSTEM_ERROR);
        }
        PostDetailVO detail = result.getData();

        PostDetailPageVO pageVO = BeanUtil.copyProperties(detail, PostDetailPageVO.class);
        pageVO.setPostId(detail.getId());
        pageVO.setAuthorUserId(detail.getUserId());

        // 2. 吧信息
        if (StrUtil.isNotBlank(detail.getBarId())) {
            Result<BarDetailVO> barResult;
            try {
                barResult = barClient.queryBar(detail.getBarId());
            } catch (FeignException e) {
                throw new ServiceException(BaseEnums.THIRD_PARTY_ERROR.getMessage(), e, BaseEnums.THIRD_PARTY_ERROR);
            }
            if (Result.SUCCESS_CODE.equals(barResult.getCode()) && barResult.getData() != null) {
                pageVO.setBarName(barResult.getData().getName());
                pageVO.setBarAvatarUrl(barResult.getData().getAvatarUrl());
            }
        }
        if (StrUtil.isBlank(pageVO.getBarName())) {
            pageVO.setBarName("未知吧");
        }

        // 3. 帖子作者昵称（本模块 user 表）
        if (StrUtil.isNotBlank(detail.getUserId())) {
            UserDO author = userMapper.selectById(detail.getUserId());
            if (author != null) {
                pageVO.setAuthorNickname(author.getNickname());
                pageVO.setAuthorAvatarUrl(author.getAvatarUrl());
            }
        }
        if (StrUtil.isBlank(pageVO.getAuthorNickname())) {
            pageVO.setAuthorNickname("未知用户");
        }
        return pageVO;
    }

    @Override
    public FloorPageResponseVO getFloors(String postId, Integer pageNum, Integer pageSize) {
        // 1. 拉分页楼层
        Result<FloorPageVO> result;
        try {
            result = postClient.listFloors(postId, pageNum, pageSize);
        } catch (FeignException e) {
            throw new ServiceException(BaseEnums.THIRD_PARTY_ERROR.getMessage(), e, BaseEnums.THIRD_PARTY_ERROR);
        }
        if (!Result.SUCCESS_CODE.equals(result.getCode())) {
            throw new ServiceException(result.getMessage(), null, BaseEnums.SYSTEM_ERROR);
        }
        FloorPageVO floorPage = result.getData();

        FloorPageResponseVO response = new FloorPageResponseVO();
        if (floorPage == null || CollUtil.isEmpty(floorPage.getFloors())) {
            response.setRecords(List.of());
            response.setTotal(0L);
            response.setTotalPages(0L);
            response.setPageNum(pageNum == null ? 1L : pageNum.longValue());
            response.setPageSize(pageSize == null ? 10L : pageSize.longValue());
            return response;
        }

        // 2. 收集楼层作者 userIds → 批量查昵称
        Set<String> userIds = new HashSet<>();
        collectFloorUserIds(floorPage.getFloors(), userIds);
        Map<String, UserDO> userMap = Map.of();
        if (!userIds.isEmpty()) {
            List<UserDO> users = userMapper.selectBatchIds(userIds);
            userMap = users.stream().collect(Collectors.toMap(UserDO::getId, u -> u));
        }

        // 3. 递归填昵称并映射为前端 VO
        List<FloorVO> records = new ArrayList<>(floorPage.getFloors().size());
        for (CommentFloorVO floor : floorPage.getFloors()) {
            records.add(buildFloorVO(floor, userMap));
        }
        response.setRecords(records);
        response.setTotal(floorPage.getTotal());
        response.setTotalPages(floorPage.getTotalPages());
        response.setPageNum(floorPage.getPageNum());
        response.setPageSize(floorPage.getPageSize());
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
    private FloorVO buildFloorVO(CommentFloorVO floor, Map<String, UserDO> userMap) {
        FloorVO vo = BeanUtil.copyProperties(floor, FloorVO.class, "children");
        UserDO user = userMap.get(floor.getUserId());
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

    @Override
    public CreateCommentVO createComment(String postId, CreateCommentRequest request) {
        Result<CreateCommentVO> result;
        try {
            result = postClient.createComment(postId, request);
        } catch (FeignException e) {
            throw new ServiceException(BaseEnums.THIRD_PARTY_ERROR.getMessage(), e, BaseEnums.THIRD_PARTY_ERROR);
        }
        if (!Result.SUCCESS_CODE.equals(result.getCode())) {
            throw new ServiceException(result.getMessage(), null, BaseEnums.SYSTEM_ERROR);
        }
        return result.getData();
    }

    @Override
    public CreatePostVO createPost(String barId, CreatePostRequest request) {
        Result<CreatePostVO> result;
        try {
            result = postClient.createPost(barId, request);
        } catch (FeignException e) {
            throw new ServiceException(BaseEnums.THIRD_PARTY_ERROR.getMessage(), e, BaseEnums.THIRD_PARTY_ERROR);
        }
        if (!Result.SUCCESS_CODE.equals(result.getCode())) {
            throw new ServiceException(result.getMessage(), null, BaseEnums.SYSTEM_ERROR);
        }
        return result.getData();
    }

    @Override
    public Long postLike(String postId) {
        Result<Long> result;
        try {
            result = postClient.postLike(postId);
        } catch (FeignException e) {
            throw new ServiceException(BaseEnums.THIRD_PARTY_ERROR.getMessage(), e, BaseEnums.THIRD_PARTY_ERROR);
        }
        if (!Result.SUCCESS_CODE.equals(result.getCode())) {
            throw new ServiceException(result.getMessage(), null, BaseEnums.SYSTEM_ERROR);
        }
        return result.getData();
    }

    @Override
    public Long postUnlike(String postId) {
        Result<Long> result;
        try {
            result = postClient.postUnlike(postId);
        } catch (FeignException e) {
            throw new ServiceException(BaseEnums.THIRD_PARTY_ERROR.getMessage(), e, BaseEnums.THIRD_PARTY_ERROR);
        }
        if (!Result.SUCCESS_CODE.equals(result.getCode())) {
            throw new ServiceException(result.getMessage(), null, BaseEnums.SYSTEM_ERROR);
        }
        return result.getData();
    }

    @Override
    public Boolean postLiked(String postId) {
        Result<Boolean> result;
        try {
            result = postClient.postLiked(postId);
        } catch (FeignException e) {
            throw new ServiceException(BaseEnums.THIRD_PARTY_ERROR.getMessage(), e, BaseEnums.THIRD_PARTY_ERROR);
        }
        if (!Result.SUCCESS_CODE.equals(result.getCode())) {
            throw new ServiceException(result.getMessage(), null, BaseEnums.SYSTEM_ERROR);
        }
        return result.getData();
    }

    @Override
    public Long postCollect(String postId) {
        Result<Long> result;
        try {
            result = postClient.postCollect(postId);
        } catch (FeignException e) {
            throw new ServiceException(BaseEnums.THIRD_PARTY_ERROR.getMessage(), e, BaseEnums.THIRD_PARTY_ERROR);
        }
        if (!Result.SUCCESS_CODE.equals(result.getCode())) {
            throw new ServiceException(result.getMessage(), null, BaseEnums.SYSTEM_ERROR);
        }
        return result.getData();
    }

    @Override
    public Long postUncollect(String postId) {
        Result<Long> result;
        try {
            result = postClient.postUncollect(postId);
        } catch (FeignException e) {
            throw new ServiceException(BaseEnums.THIRD_PARTY_ERROR.getMessage(), e, BaseEnums.THIRD_PARTY_ERROR);
        }
        if (!Result.SUCCESS_CODE.equals(result.getCode())) {
            throw new ServiceException(result.getMessage(), null, BaseEnums.SYSTEM_ERROR);
        }
        return result.getData();
    }

    @Override
    public Boolean postCollected(String postId) {
        Result<Boolean> result;
        try {
            result = postClient.postCollected(postId);
        } catch (FeignException e) {
            throw new ServiceException(BaseEnums.THIRD_PARTY_ERROR.getMessage(), e, BaseEnums.THIRD_PARTY_ERROR);
        }
        if (!Result.SUCCESS_CODE.equals(result.getCode())) {
            throw new ServiceException(result.getMessage(), null, BaseEnums.SYSTEM_ERROR);
        }
        return result.getData();
    }

    @Override
    public Long commentLike(String commentId) {
        Result<Long> result;
        try {
            result = postClient.commentLike(commentId);
        } catch (FeignException e) {
            throw new ServiceException(BaseEnums.THIRD_PARTY_ERROR.getMessage(), e, BaseEnums.THIRD_PARTY_ERROR);
        }
        if (!Result.SUCCESS_CODE.equals(result.getCode())) {
            throw new ServiceException(result.getMessage(), null, BaseEnums.SYSTEM_ERROR);
        }
        return result.getData();
    }

    @Override
    public Long commentUnlike(String commentId) {
        Result<Long> result;
        try {
            result = postClient.commentUnlike(commentId);
        } catch (FeignException e) {
            throw new ServiceException(BaseEnums.THIRD_PARTY_ERROR.getMessage(), e, BaseEnums.THIRD_PARTY_ERROR);
        }
        if (!Result.SUCCESS_CODE.equals(result.getCode())) {
            throw new ServiceException(result.getMessage(), null, BaseEnums.SYSTEM_ERROR);
        }
        return result.getData();
    }

    @Override
    public Boolean commentLiked(String commentId) {
        Result<Boolean> result;
        try {
            result = postClient.commentLiked(commentId);
        } catch (FeignException e) {
            throw new ServiceException(BaseEnums.THIRD_PARTY_ERROR.getMessage(), e, BaseEnums.THIRD_PARTY_ERROR);
        }
        if (!Result.SUCCESS_CODE.equals(result.getCode())) {
            throw new ServiceException(result.getMessage(), null, BaseEnums.SYSTEM_ERROR);
        }
        return result.getData();
    }

    @Override
    public void deleteComment(String commentId) {
        Result<Void> result;
        try {
            result = postClient.commentDelete(commentId);
        } catch (FeignException e) {
            throw new ServiceException(BaseEnums.THIRD_PARTY_ERROR.getMessage(), e, BaseEnums.THIRD_PARTY_ERROR);
        }
        if (!Result.SUCCESS_CODE.equals(result.getCode())) {
            throw new ServiceException(result.getMessage(), null, BaseEnums.SYSTEM_ERROR);
        }
    }

    @Override
    public Long createBar(CreateBarRequest request) {
        Result<Long> result;
        try {
            result = barClient.createBar(request);
        } catch (FeignException e) {
            throw new ServiceException(BaseEnums.THIRD_PARTY_ERROR.getMessage(), e, BaseEnums.THIRD_PARTY_ERROR);
        }
        if (!Result.SUCCESS_CODE.equals(result.getCode())) {
            throw new ServiceException(result.getMessage(), null, BaseEnums.SYSTEM_ERROR);
        }
        return result.getData();
    }

    @Override
    public BarDetailVO queryBar(String barId) {
        Result<BarDetailVO> result;
        try {
            result = barClient.queryBar(barId);
        } catch (FeignException e) {
            throw new ServiceException(BaseEnums.THIRD_PARTY_ERROR.getMessage(), e, BaseEnums.THIRD_PARTY_ERROR);
        }
        if (!Result.SUCCESS_CODE.equals(result.getCode())) {
            throw new ServiceException(result.getMessage(), null, BaseEnums.SYSTEM_ERROR);
        }
        return result.getData();
    }

    @Override
    public Long followBar(String barId) {
        Result<Long> result;
        try {
            result = barClient.followBar(barId);
        } catch (FeignException e) {
            throw new ServiceException(BaseEnums.THIRD_PARTY_ERROR.getMessage(), e, BaseEnums.THIRD_PARTY_ERROR);
        }
        if (!Result.SUCCESS_CODE.equals(result.getCode())) {
            throw new ServiceException(result.getMessage(), null, BaseEnums.SYSTEM_ERROR);
        }
        return result.getData();
    }

    @Override
    public Long unfollowBar(String barId) {
        Result<Long> result;
        try {
            result = barClient.unfollowBar(barId);
        } catch (FeignException e) {
            throw new ServiceException(BaseEnums.THIRD_PARTY_ERROR.getMessage(), e, BaseEnums.THIRD_PARTY_ERROR);
        }
        if (!Result.SUCCESS_CODE.equals(result.getCode())) {
            throw new ServiceException(result.getMessage(), null, BaseEnums.SYSTEM_ERROR);
        }
        return result.getData();
    }

    @Override
    public Boolean isFollowed(String barId) {
        Result<Boolean> result;
        try {
            result = barClient.isFollowed(barId);
        } catch (FeignException e) {
            throw new ServiceException(BaseEnums.THIRD_PARTY_ERROR.getMessage(), e, BaseEnums.THIRD_PARTY_ERROR);
        }
        if (!Result.SUCCESS_CODE.equals(result.getCode())) {
            throw new ServiceException(result.getMessage(), null, BaseEnums.SYSTEM_ERROR);
        }
        return result.getData();
    }

    @Override
    public void getPost(String postId) {


    }
}
