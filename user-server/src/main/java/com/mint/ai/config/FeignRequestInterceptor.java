package com.mint.ai.config;

import com.mint.ai.common.coontext.UserContext;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign调用前的操作
 */
@Configuration
public class FeignRequestInterceptor {

    /**
     * RequestInterceptor 是 feign 库的接口（feign.RequestInterceptor），
     * Spring Cloud OpenFeign 会自动收集所有该类型的 Bean，作用于每次 Feign 调用。
     * @return
     */
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
