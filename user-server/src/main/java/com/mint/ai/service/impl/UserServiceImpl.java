package com.mint.ai.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
import com.mint.ai.post.api.vo.CreateCommentVO;
import com.mint.ai.post.api.vo.CreatePostVO;
import com.mint.ai.post.api.vo.PostSummaryVO;
import com.mint.ai.service.UserService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    private final PostClient postClient;
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
}
