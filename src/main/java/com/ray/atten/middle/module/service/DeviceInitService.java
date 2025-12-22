package com.ray.atten.middle.module.service;

import com.ray.atten.middle.module.model.Device;
import com.ray.atten.middle.module.repository.DeviceRepository;
import com.ray.atten.middle.module.utils.DeviceHeartbeatHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Service
public class DeviceInitService {

    @Autowired
    private DeviceRepository deviceRepository;

    /**
     * 项目启动后自动执行
     */
    @PostConstruct
    public void initDeviceStatus() {
        // 1. 系统重启时，将所有原本在线的状态暂时重置为在线（假设它们还在运行）
        // 或者保持数据库状态，等待设备下一次 PUSH 请求来激活
        List<Device> activeDevices = deviceRepository.findByActiveTrue();

        activeDevices.forEach(device -> {
            // 如果你希望启动时默认它们是在线的，可以预热缓存：
            DeviceHeartbeatHolder.refresh(device.getDeviceSn());
            log.info("系统初始化：监控活跃设备 [{}]", device.getDeviceSn());
        });
    }

}
