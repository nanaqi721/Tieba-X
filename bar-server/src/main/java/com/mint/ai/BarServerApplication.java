package com.mint.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * bar模块启动类
 */
@SpringBootApplication
@MapperScan("com.mint.ai.mapper")
public class BarServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(BarServerApplication.class,args);
    }
}
