package com.mint.ai.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关统一登录态透传：把 sa-token 登录用户 id 翻译成下游 post/bar 服务认识的 X-User-Id 请求头。
 * <p>
 * 前置 SaReactorFilter（WebFilter，order -100）已做登录校验；本过滤器负责注入身份头。
 * 匿名白名单请求无登录态，原样放行（不注入）。
 */
@Component
public class UserIdHeaderFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        try {
            SaReactorSyncHolder.setContext(exchange);
            long userId = StpUtil.getLoginIdAsLong();
            ServerWebExchange mutated = exchange.mutate()
                    .request(builder -> builder.header("X-User-Id", String.valueOf(userId)))
                    .build();
            return chain.filter(mutated);
        } catch (NotLoginException e) {
            // 匿名白名单请求：无登录态，原样放行
            return chain.filter(exchange);
        } finally {
            SaReactorSyncHolder.clearContext();
        }
    }

    @Override
    public int getOrder() {
        return -50;
    }
}
