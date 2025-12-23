package com.ray.atten.middle.module.service;

import com.ray.atten.middle.module.dto.SysConfigRequest;
import com.ray.atten.middle.module.model.SysConfig;
import com.ray.atten.middle.module.repository.SysConfigRepository;
import com.ray.atten.middle.module.utils.GlobalConfigHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Service
public class SysConfigService {

    @Autowired
    private SysConfigRepository configRepository;

    /**
     * 项目启动时自动加载数据库所有配置到内存
     */
    @PostConstruct
    public void initConfigCache() {
        refreshCache();
    }

    /**
     * 刷新缓存方法：从数据库读取并覆盖内存
     */
    public void refreshCache() {
        List<SysConfig> configs = configRepository.findAll();
        for (SysConfig config : configs) {
            GlobalConfigHolder.set(config.getConfigKey(), config.getConfigValue());
        }
        log.debug(">>> [系统配置] 内存缓存已刷新");
    }

    /**
     * 保存或更新配置
     */
    @Transactional
    public void saveOrUpdateConfig(SysConfigRequest request) {
        SysConfig config = configRepository.findByConfigKey(request.getKey());
        if (config == null) {
            config = new SysConfig();
            config.setConfigKey(request.getKey());
        }
        config.setConfigValue(request.getValue());
        config.setConfigDesc(request.getDesc());
        configRepository.save(config);

        // 关键：数据库改完，立即刷内存
        refreshCache();
    }
}
