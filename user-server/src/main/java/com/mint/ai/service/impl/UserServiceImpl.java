package com.mint.ai.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mint.ai.common.Result;
import com.mint.ai.common.coontext.UserContext;
import com.mint.ai.common.dto.CreateUserRequest;
import com.mint.ai.common.dto.LoginRequest;
import com.mint.ai.common.enums.UserErrorCode;
import com.mint.ai.common.exception.ClientException;
import com.mint.ai.common.redisKey.RedisConstantKey;
import com.mint.ai.common.vo.UserVO;
import com.mint.ai.user.api.vo.UserBaseVO;
import com.mint.ai.mapper.UserMapper;
import com.mint.ai.mapper.entity.UserDO;
import com.mint.ai.service.UserService;
import com.mint.ai.util.MyMapUtils;
import com.mint.ai.utils.Results;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final RedissonClient redissonClient;

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
    public Map<String, UserBaseVO> batchGetUsersByIds(List<String> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Map.of();
        }
        List<UserDO> users = userMapper.selectBatchIds(ids);
        return users.stream().collect(Collectors.toMap(UserDO::getId, v -> BeanUtil.toBean(v, UserBaseVO.class)));
    }

    @Override
    public UserVO getUserInfo() {

        String userId = UserContext.getUserId();
        Map<Object, Object> userCache = stringRedisTemplate.opsForHash().entries(String.format(RedisConstantKey.USER_CACHE, userId));

        Map<String,String> newUserCache = MyMapUtils.mapToStingMap(userCache);

        if(!userCache.isEmpty()){
            return BeanUtil.mapToBean(newUserCache,UserVO.class,true);
        }

        String userNullCache = stringRedisTemplate.opsForValue().get(String.format(String.format(RedisConstantKey.USER_NULL_CACHE)));
        if(StrUtil.isNotEmpty(userNullCache)){
            throw new ClientException("用户不存在请查询有效用户");
        }
        // 尝试获取分布式锁
        RLock lock = redissonClient.getLock(String.format(RedisConstantKey.USER_CACHE_LOCK));
        try {
            if(lock.tryLock(2, TimeUnit.MINUTES)){
                // 检查是否有空缓存构建了
                userNullCache = stringRedisTemplate.opsForValue().get(String.format(String.format(RedisConstantKey.USER_NULL_CACHE)));
                if(StrUtil.isNotEmpty(userNullCache)){
                    throw new ClientException("用户不存在请查询有效用户");
                }

                // 检查是否有缓存构建了
                userCache = stringRedisTemplate.opsForHash().entries(String.format(RedisConstantKey.USER_CACHE, userId));
                newUserCache = MyMapUtils.mapToStingMap(userCache);
                if(!userCache.isEmpty()){
                    return BeanUtil.mapToBean(newUserCache,UserVO.class,true);
                }

                //开始构建缓存

                UserDO userDO = userMapper.selectById(userId);
                if(userDO == null){
                    stringRedisTemplate.opsForValue().set(String.format(String.format(RedisConstantKey.USER_NULL_CACHE))," ");
                    throw new ClientException("用户不存在");
                }

                UserVO userVO = BeanUtil.toBean(userDO, UserVO.class);
                Map<String, Object> stringObjectMap = BeanUtil.beanToMap(userVO);
                Map<String, String> userCacheMap = new HashMap<>();
                for(String key : stringObjectMap.keySet()){
                    userCacheMap.put(key,String.valueOf(stringObjectMap.get(key)));
                }
                stringRedisTemplate.opsForHash().putAll(String.format(RedisConstantKey.USER_CACHE,userId),userCacheMap);
                return userVO;

            }
        } catch (Exception e) {
            throw new ClientException("查询用户失败请稍后重试");
        } finally {
            // 如果锁是当前线程获取的才释放，防止误删锁
            if(lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        UserDO userDO = userMapper.selectById(userId);
        if(userDO == null){
            throw new ClientException("用户不存在");
        }
        return BeanUtil.toBean(userDO,UserVO.class);
    }
}
