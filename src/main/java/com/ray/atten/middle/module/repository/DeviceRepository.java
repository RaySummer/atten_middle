package com.ray.atten.middle.module.repository;

import com.ray.atten.middle.module.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> , JpaSpecificationExecutor<Device> {

    // 根据序列号查找设备，用于判断是新增还是更新
    Optional<Device> findByDeviceSn(String deviceSn);

    List<Device> findAllByActiveTrue();

    /**
     * 【新增】根據一組設備序列號批量查詢設備實體
     */
    List<Device> findByDeviceSnIn(List<String> sns);

}
