package com.ray.atten.middle.module.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DeviceHeartbeatHolder {

    // 静态缓存实例
    private static Cache<String, LocalDateTime> heartbeatCache;

    /**
     * 初始化缓存配置
     * 建议过期时间设置为设备 PUSH Delay 的 2.5 倍
     */
    static {
        heartbeatCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES) // 5分钟未更新则失效
                .maximumSize(1000)
                .build();
    }

    /**
     * 更新设备最后在线时间（设备请求时调用）
     */
    public static void refresh(String sn) {
        heartbeatCache.put(sn, LocalDateTime.now());
    }

    /**
     * 检查设备是否在缓存中（即判断是否在线）
     */
    public static boolean isOnline(String sn) {
        return heartbeatCache.getIfPresent(sn) != null;
    }

    /**
     * 手动移除（如删除设备时调用）
     */
    public static void remove(String sn) {
        heartbeatCache.invalidate(sn);
    }

    /**
     * 获取指定设备的最后在线时间（从内存获取，极快）
     */
    public static LocalDateTime getLastSeen(String sn) {
        return heartbeatCache.getIfPresent(sn);
    }

}
