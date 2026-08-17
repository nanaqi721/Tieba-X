package com.mint.ai.config;

import cn.hutool.core.util.StrUtil;
import com.mint.ai.common.coontext.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器（宽松版）：从 X-User-Id 请求头提取用户 id 写入用户上下文。
 * <p>
 * 只填充不阻断——鉴权已由网关 SaReactorFilter 全局 checkLogin + 本服务 SaInterceptor 注解鉴权双层保证；
 * 且 bar-server 等内部 Feign 调用 /batch 时并不携带 X-User-Id，严格版会误拦内部调用。
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader("X-User-Id");
        if (StrUtil.isNotBlank(userId)) {
            UserContext.setUserId(userId);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 防止 ThreadLocal 内存泄漏
        UserContext.removeUserId();
    }
}
