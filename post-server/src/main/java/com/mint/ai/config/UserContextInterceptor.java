package com.mint.ai.config;

import cn.hutool.core.util.StrUtil;
import com.mint.ai.common.coontext.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户上下文拦截器：读取请求头 X-User-Id 写入 UserContext。
 * 调用方（user-server 的 FeignRequestInterceptor）已自动带上该头，post-server 只负责读取。
 */
@Component
public class UserContextInterceptor implements HandlerInterceptor {

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
