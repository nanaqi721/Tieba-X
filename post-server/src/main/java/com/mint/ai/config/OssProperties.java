package com.mint.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssProperties {

    /**
     * OSS 地域节点
     */
    private String endpoint;

    private String accessKeyId;

    private String accessKeySecret;

    /**
     * 存储空间名
     */
    private String bucket;
}
