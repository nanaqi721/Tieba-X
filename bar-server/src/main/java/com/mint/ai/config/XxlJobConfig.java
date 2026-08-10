package com.mint.ai.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * xxl-job配置类
 */
@Configuration
@EnableConfigurationProperties(XxlJobConfig.Properties.class)
public class XxlJobConfig {

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor(Properties properties){

        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(properties.getAdmin().getAddresses());
        executor.setAccessToken(properties.getAccessToken());
        executor.setAppname(properties.getExecutor().getAppName());
        executor.setIp(properties.getExecutor().getIp());
        executor.setPort(properties.getExecutor().getPort());
        executor.setLogPath(properties.getExecutor().getLogPath());
        executor.setLogRetentionDays(properties.getExecutor().getLogRetentionDays());
        return executor;


    }


    @Data
    @ConfigurationProperties(prefix = "xxl.job")
    public static class Properties {

        private String accessToken;

        private Admin admin = new Admin();

        private Executor executor = new Executor();

        @Data
        public static class Admin {

            private String addresses;
        }

        @Data
        public static class Executor {

            private String appName;

            private String ip;

            private int port;

            private String logPath;

            private int logRetentionDays;
        }


    }

}
