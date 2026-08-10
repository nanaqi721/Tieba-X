package com.mint.ai.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mint.ai.common.coontext.UserContext;
import com.mint.ai.common.enums.BaseEnums;
import com.mint.ai.utils.Results;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.pattern.PathPattern;

import java.io.IOException;
import java.util.List;

/**
 * 登录拦截器：白名单（公开 GET 浏览接口）放行，其余接口必须携带 X-User-Id。
 * 已登录（携带 X-User-Id，由网关/user-server 的 Feign 拦截器注入）时写入用户上下文。
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    /** 白名单：无需登录的公开浏览接口。bar 当前只有创建吧需登录，暂无公开浏览接口，留空。 */
    private static final List<PathPattern> WHITE_LIST = List.of();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        // 登录了就把用户 id 写入上下文；白名单浏览或已登录都放行
        String userId = request.getHeader("X-User-Id");
        boolean hasLogin = StrUtil.isNotBlank(userId);
        if (hasLogin) {
            UserContext.setUserId(userId);
        }
        if (hasLogin) {
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
}
