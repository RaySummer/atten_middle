package com.ray.atten.middle.module.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public Cache<String, LocalDateTime> heartbeatCache() {
        return Caffeine.newBuilder()
                // 設置寫入後 5 分鐘過期（根據你的 Delay 參數調整，建議大於 2 倍 Delay）
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000) // 最大存儲 1000 台設備
                .build();
    }
}
