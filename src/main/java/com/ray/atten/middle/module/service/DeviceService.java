package com.ray.atten.middle.module.service;

import com.ray.atten.middle.module.dto.DeviceRequest;
import com.ray.atten.middle.module.model.Device;
import com.ray.atten.middle.module.repository.DeviceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class DeviceService {

    @Autowired
    private DeviceRepository deviceRepository;

    /**
     * 新增或更新考勤机信息
     *
     * @param request 前端传入的设备信息
     * @return 保存后的 Device 实体
     */
    public Device saveOrUpdateDevice(DeviceRequest request) {

        // 1. 尝试根据SN查找现有设备
        Optional<Device> existingDevice = deviceRepository.findByDeviceSn(request.getDeviceSn());

        Device device;

        if (existingDevice.isPresent()) {
            // 2. 如果设备已存在，则更新
            device = existingDevice.get();
        } else {
            // 3. 如果设备不存在，则创建新记录
            device = new Device();
            device.setDeviceSn(request.getDeviceSn()); // 序列号只能在创建时设置
        }

        // 4. 更新字段
        device.setAlias(request.getAlias());
        device.setLocation(request.getLocation());
        device.setModel(request.getModel());
        device.setIpAddress(request.getIpAddress());
        device.setActive(request.isActive());

        // 5. 保存到数据库 (利用 @PreUpdate 自动更新 updateTime)
        return deviceRepository.save(device);
    }

    public List<Device> findAllDevices() {
        return deviceRepository.findAll();
    }

}
