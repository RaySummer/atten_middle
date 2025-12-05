package com.ray.atten.middle.module.controller;

import com.ray.atten.middle.module.dto.DeviceRequest;
import com.ray.atten.middle.module.model.Device;
import com.ray.atten.middle.module.service.DeviceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/management/devices")
public class DeviceManagementController {

    @Autowired
    private DeviceService deviceService;

    /**
     * 接口功能：新增或修改考勤机信息
     * 使用 POST 或 PUT 方法，根据 SN 自动判断是创建还是更新
     * URL: POST /api/management/devices
     *
     * @param request 包含设备信息和配置的 DTO
     * @return 保存后的设备实体
     */
    @PostMapping
    public ResponseEntity<Device> saveOrUpdateDevice(@Valid @RequestBody DeviceRequest request) {

        Device savedDevice = deviceService.saveOrUpdateDevice(request);

        return ResponseEntity.ok(savedDevice);
    }

    // 增加一个 GET 接口用于查询所有设备
    @GetMapping
    public ResponseEntity<Iterable<Device>> getAllDevices() {
        return ResponseEntity.ok(deviceService.findAllDevices());
    }
}
