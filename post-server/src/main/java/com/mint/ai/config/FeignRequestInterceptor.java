package com.mint.ai.config;

import com.mint.ai.common.coontext.UserContext;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign调用前的操作：把当前登录用户 id 以 X-User-Id 头带到下游服务
 */
@Configuration
public class FeignRequestInterceptor {

    @Bean
    public RequestInterceptor userContextRequestInterceptor() {
        return Template -> {
            String userId = UserContext.getUserId();
            if(userId != null){
                Template.header("X-User-Id",userId);
            }
        };
    }
}
