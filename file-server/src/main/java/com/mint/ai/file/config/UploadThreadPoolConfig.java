package com.mint.ai.file.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * OSS 并行上传线程池
 *
 * 任务性质：IO 密集型（线程几乎都在等 OSS 网络往返），单任务 ≤5MB，每请求最多 5 个任务，
 * 因此线程数可以远大于 CPU 核数，上限由并发 OSS 连接数约束。
 *
 * 关键点：workQueue 必须用 SynchronousQueue，否则 ThreadPoolExecutor 的扩容顺序是
 * "先到 core → 排进队列 → 队列满才扩到 max"，任务会全排进队列而只跑 core 个线程，并行优化失效。
 */
@Configuration
public class UploadThreadPoolConfig {

    @Bean("ossUploadExecutor")
    public ThreadPoolExecutor ossUploadExecutor() {
        return new ThreadPoolExecutor(
                8,                       // 核心线程数
                30,                                  // 最大线程数 5 * 6
                60L,                                 // 救急线程存活时间
                TimeUnit.SECONDS,                    // unit 时间单位
                new SynchronousQueue<>(),            // workQueue 不缓冲，任务直驱建线程→真并行
                r -> {                               // threadFactory 自定义线程名，便于排查
                    Thread t = new Thread(r);
                    t.setName("oss-upload-" + t.getId());
                    t.setDaemon(false);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // handler 满负荷背压，不丢任务不返500
        );
    }
}
