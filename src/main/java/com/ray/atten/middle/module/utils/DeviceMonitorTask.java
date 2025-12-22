package com.ray.atten.middle.module.utils;

import com.ray.atten.middle.module.model.Device;
import com.ray.atten.middle.module.service.DeviceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class DeviceMonitorTask {

    @Autowired
    private DeviceService deviceService;

    // 每 1 分鐘檢查一次
    @Scheduled(fixedRate = 60000)
    public void checkOfflineDevices() {
        // 扫描所有【用户开启了监控】的设备
        List<Device> activeDevices = deviceService.findDeviceIsTrue();

        for (Device device : activeDevices) {
            String sn = device.getDeviceSn();

            // 核心逻辑：如果在内存缓存里找不到了
            if (!DeviceHeartbeatHolder.isOnline(sn)) {

                // 额外判断：如果数据库里还显示是在线(true)或者是初始状态(null)，则必须执行离线操作
                if (device.getIsOnline() == null || device.getIsOnline()) {
                    // 调用设置离线的方法
                    deviceService.setDeviceOffline(sn);
                }
            }
        }
    }
}
