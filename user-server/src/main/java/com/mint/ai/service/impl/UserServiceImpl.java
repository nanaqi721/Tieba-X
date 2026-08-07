package com.mint.ai.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mint.ai.common.dto.CreateUserRequest;
import com.mint.ai.common.dto.LoginRequest;
import com.mint.ai.common.enums.UserErrorCode;
import com.mint.ai.common.execption.ClientException;
import com.mint.ai.mapper.UserMapper;
import com.mint.ai.mapper.entiy.UserDO;
import com.mint.ai.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
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
}
