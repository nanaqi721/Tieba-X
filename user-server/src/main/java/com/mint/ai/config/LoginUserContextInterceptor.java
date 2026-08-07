package com.mint.ai.config;

import cn.dev33.satoken.stp.StpUtil;
import com.mint.ai.common.coontext.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户登陆上下文拦截器
 */
@Component
public class LoginUserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 如果登陆了就设置用户上下文id,无论如何都放行，这个拦截器只设置用户上下文，拦截登陆交给sa-token
        if(StpUtil.isLogin()){
            UserContext.setUserId(StpUtil.getLoginId().toString());
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // controller执行完毕防止内存泄漏执行remove
        UserContext.removeUserId();
    }
}
