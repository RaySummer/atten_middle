package com.ray.atten.middle.module.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 異步任務配置，啟用 @Async 註解，並配置專門的線程池。
 */
@Configuration
@EnableAsync // 啟用 Spring 對 @Async 註解的支持
public class AsyncConfig {

    @Bean(name = "employeePushExecutor")
    public Executor employeePushExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心線程數：即使空閒也保持運行的線程數
        executor.setCorePoolSize(5);
        // 最大線程數：線程池允許的最大線程數
        executor.setMaxPoolSize(10);
        // 隊列容量：線程滿載時，新任務等待的隊列大小
        executor.setQueueCapacity(1000);
        // 線程名稱前綴：方便日誌中區分
        executor.setThreadNamePrefix("EmployeePush-");
        // 初始化線程池
        executor.initialize();
        return executor;
    }

    // 考勤機數據處理專用線程池 (防止考勤機數據量大時阻塞其他業務)
    @Bean(name = "iClockExecutor")
    public Executor iClockExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 考勤機數據量大，可以給予更多的線程資源
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(2000); // 增加隊列以處理突發的大數據量
        executor.setThreadNamePrefix("IClock-Processor-");
        executor.initialize();
        return executor;
    }

    // 如果不指定執行器，可以使用這個默認的作為備用
    @Bean
    public Executor taskExecutor() {
        return new ThreadPoolTaskExecutor();
    }
}
