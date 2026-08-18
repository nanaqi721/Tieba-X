package com.mint.ai.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Sa-Token 网关统一鉴权配置
 * <p>
 * 基于 WebFlux 的 {@link SaReactorFilter} 全局过滤器：
 * 白名单接口放行，其余全部要求登录。校验通过后 satoken 请求头原样透传给下游服务，
 * 由下游 servlet 服务自行用 StpUtil 读取登录用户信息。
 * 网关与下游共用同一 Redis（sa-token-redis-template），因此能直接校验登录态。
 */
@Configuration

public class SaTokenConfigure {

    private final ObjectMapper objectMapper;

    public SaTokenConfigure(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 匿名接口白名单：登录 / 注册 / 首页信息流展示
     * 其余接口（发帖、评论、点赞、收藏、关注、上传等）全部要求登录
     */
    private static final String[] EXCLUDE_PATHS = {
            "/api/users/v1/login",
            "/api/users/v1/register",
            "/api/posts/v1/feed",
            "/api/posts/v1/detail",
            "/api/posts/v1/floors",
            "/api/posts/v1/floors/**",
    };

    @Bean
    public SaReactorFilter saReactorFilter() {
        return new SaReactorFilter()
                // 拦截所有请求
                .addInclude("/**")
                // 匿名接口白名单
                .addExclude(EXCLUDE_PATHS)
                // 认证函数：白名单之外的请求全部校验登录
                .setAuth(obj -> {
                    SaRouter.match("/**", r -> StpUtil.checkLogin());
                    // 后续可按需追加角色/权限校验，例如：
                    // SaRouter.match("/api/admin/**", r -> StpUtil.checkRole("admin"));
                })
                // 异常处理：未登录等统一返回 JSON，保持与下游 {code, message, data} 结构一致
                .setError(e -> {
                    boolean notLogin = e instanceof NotLoginException;
                    int status = notLogin ? HttpStatus.UNAUTHORIZED.value() : HttpStatus.INTERNAL_SERVER_ERROR.value();
                    SaHolder.getResponse().setStatus(status);
                    SaHolder.getResponse().setHeader("Content-Type", "application/json;charset=UTF-8");
                    return toJson(status, e.getMessage());
                });
    }

    private String toJson(int code, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", String.valueOf(code));
        body.put("message", message);
        body.put("data", null);
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            return "{\"code\":\"401\",\"message\":\"未登录\",\"data\":null}";
        }
    }
}
