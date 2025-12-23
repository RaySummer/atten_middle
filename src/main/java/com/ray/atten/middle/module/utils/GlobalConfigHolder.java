package com.ray.atten.middle.module.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class GlobalConfigHolder {

    private static final ConcurrentHashMap<String, String> CONFIG_CACHE = new ConcurrentHashMap<>();

    // 存入配置
    public static void set(String key, String value) {
        if (key != null && value != null) {
            CONFIG_CACHE.put(key, value);
        }
    }

    // 获取配置
    public static String get(String key) {
        return CONFIG_CACHE.get(key);
    }

    // 默认值获取
    public static String getOrDefault(String key, String defaultValue) {
        return CONFIG_CACHE.getOrDefault(key, defaultValue);
    }

    // 专门为更新目录提供的便捷方法
    public static String getUpdateDir() {
        return getOrDefault("UPDATE_FILES_DIR", "D:/atten_updates/");
    }

}
