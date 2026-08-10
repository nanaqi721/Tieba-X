package com.mint.ai.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mint.ai.common.coontext.UserContext;
import com.mint.ai.common.enums.BaseEnums;
import com.mint.ai.utils.Results;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.IOException;
import java.util.List;

/**
 * 登录拦截器：白名单（公开 GET 浏览接口）放行，其余接口必须携带 X-User-Id。
 * 已登录（携带 X-User-Id，由网关/user-server 的 Feign 拦截器注入）时写入用户上下文。
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private static final PathPatternParser PARSER = new PathPatternParser();

    /** 白名单：无需登录的公开浏览接口。GET 帖子摘要/详情/楼层 + 主页 feed（feed 为两段路径需单独列出）。 */
    private static final List<PathPattern> WHITE_LIST = List.of(
            PARSER.parse("/api/posts/v1/*"),
            PARSER.parse("/api/posts/v1/home/feed")
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        // 登录了就把用户 id 写入上下文；白名单浏览或已登录都放行
        String userId = request.getHeader("X-User-Id");
        boolean hasLogin = StrUtil.isNotBlank(userId);
        if (hasLogin) {
            UserContext.setUserId(userId);
        }
        if (isWhiteList(request.getMethod(), request.getServletPath()) || hasLogin) {
            return true;
        }
        // 未登录：HTTP 200 + 业务错误码，与全局异常约定一致
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSONUtil.toJsonStr(Results.error(BaseEnums.UNAUTHORIZED)));
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 防止 ThreadLocal 内存泄漏
        UserContext.removeUserId();
    }

    private boolean isWhiteList(String method, String servletPath) {
        if (!"GET".equalsIgnoreCase(method)) {
            return false;
        }
        return WHITE_LIST.stream().anyMatch(p -> p.matches(PathContainer.parsePath(servletPath)));
    }
}
