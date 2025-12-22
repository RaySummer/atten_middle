package com.ray.atten.middle.module.service;

import com.ray.atten.middle.module.dto.DeviceDto;
import com.ray.atten.middle.module.dto.DeviceRequest;
import com.ray.atten.middle.module.model.Device;
import com.ray.atten.middle.module.repository.DeviceRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    public DeviceDto saveOrUpdateDevice(DeviceRequest request) {

        Device device;
        if (request.getDeviceSn() != null) {
            // 根据 SN 查找现有设备，实现“覆盖式更新”或“新增”
            device = deviceRepository.findByDeviceSn(request.getDeviceSn())
                    .orElse(new Device());
        } else {
            device = new Device();
        }

        device.setDeviceSn(request.getDeviceSn()); // 序列号只能在创建时设置

        // 4. 更新字段
        device.setAlias(request.getAlias());
        device.setLocation(request.getLocation());
        device.setModel(request.getModel());
        device.setIpAddress(request.getIpAddress());
        device.setActive(request.getActive());

        // 5. 保存到数据库 (利用 @PreUpdate 自动更新 updateTime)
        return DeviceDto.convertToDto(deviceRepository.save(device));
    }

    public List<DeviceDto> findAllDevices() {
        return DeviceDto.convertToList(deviceRepository.findAll());
    }

    /**
     * 根据条件查询设备列表
     */
    public List<DeviceDto> getDeviceList(DeviceRequest request) {
        Specification<Device> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. deviceSn 模糊查询 (like %sn%)
            if (StringUtils.isNotEmpty(request.getDeviceSn())) {
                predicates.add(cb.like(root.get("deviceSn"), "%" + request.getDeviceSn() + "%"));
            }

            // 2. active 状态精确查询 (只有不为 null 时才加入条件)
            if (request.getActive() != null) {
                predicates.add(cb.equal(root.get("active"), request.getActive()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Device> entityList = deviceRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "id"));

        // 3. 转换为 DTO (建议使用 BeanUtils 或 MapStruct)
        List<DeviceDto> collect = entityList.stream().map(this::convertToDto).collect(Collectors.toList());
        return collect;
    }

    /**
     * 新增或修改设备
     */
    @Transactional
    public DeviceDto saveOrUpdate(DeviceDto dto) {
        Device entity;
        if (dto.getDeviceSn() != null) {
            // 根据 SN 查找现有设备，实现“覆盖式更新”或“新增”
            entity = deviceRepository.findByDeviceSn(dto.getDeviceSn())
                    .orElse(new Device());
        } else {
            entity = new Device();
        }

        // 属性拷贝
        BeanUtils.copyProperties(dto, entity, "id"); // 不拷贝 ID，交给 JPA 处理或由 SN 确定

        Device saved = deviceRepository.save(entity);
        log.info("设备已保存: {}", saved.getDeviceSn());
        return convertToDto(saved);
    }

    /**
     * 同步考勤数据 (逻辑占位)
     */
    public boolean syncAttendance(String deviceSn) {
        log.info("正在为设备 {} 开启同步任务...", deviceSn);
        try {
            // 这里通常是调用第三方 SDK 或者多线程去拉取设备日志
            // TODO: 接入设备厂商提供的 SDK 逻辑
            return true;
        } catch (Exception e) {
            log.error("同步失败: ", e);
            return false;
        }
    }

    private DeviceDto convertToDto(Device entity) {
        if (entity == null) {
            return null;
        }
        DeviceDto dto = new DeviceDto();
        dto.setId(entity.getId());
        dto.setDeviceSn(entity.getDeviceSn());
        dto.setAlias(entity.getAlias());
        dto.setLocation(entity.getLocation());
        dto.setModel(entity.getModel());
        dto.setIpAddress(entity.getIpAddress());
        dto.setActive(entity.isActive());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());

        return dto;
    }

}
