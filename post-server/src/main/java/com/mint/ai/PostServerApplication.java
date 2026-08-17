package com.mint.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 帖子模块启动类
 */
@SpringBootApplication
@MapperScan("com.mint.ai.mapper")
@EnableFeignClients
public class PostServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(PostServerApplication.class,args);
    }
}
