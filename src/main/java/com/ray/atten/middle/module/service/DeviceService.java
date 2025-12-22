package com.ray.atten.middle.module.service;

import com.ray.atten.middle.module.dto.DeviceDto;
import com.ray.atten.middle.module.dto.DeviceRequest;
import com.ray.atten.middle.module.model.Device;
import com.ray.atten.middle.module.repository.DeviceRepository;
import com.ray.atten.middle.module.utils.DeviceHeartbeatHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.time.LocalDateTime;
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
            device = deviceRepository.findByDeviceSn(request.getDeviceSn());
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
        dto.setActive(entity.getActive());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());

        return dto;
    }

    public List<Device> findDeviceIsTrue() {
        return deviceRepository.findByActiveTrue();
    }

    /**
     * 将设备设置为离线（不活跃）状态
     */
    @Transactional // 涉及更新，建议开启事务
    public void setDeviceOffline(String sn) {
        Device device = deviceRepository.findByDeviceSn(sn);

        if (device != null) {
            // 从内存拿最后一次心跳时间
            LocalDateTime realLastSeen = DeviceHeartbeatHolder.getLastSeen(sn);

            log.warn("执行离线处理 - 设备: {}, 内存最后心跳: {}", sn, realLastSeen);

            device.setIsOnline(false);
            device.setActive(false); // 自动关闭活跃状态

            // 如果内存里有时间，存内存的；如果没有（说明从来没连上过），存当前时间
            if (realLastSeen != null) {
                device.setLastSeen(realLastSeen);
            } else if (device.getLastSeen() == null) {
                device.setLastSeen(LocalDateTime.now());
            }

            deviceRepository.save(device);
            // 清理缓存，防止重复触发
            DeviceHeartbeatHolder.remove(sn);
        }
    }

    public void processHeartbeat(String sn) {
        // 1. 无论如何，先刷新内存缓存
        DeviceHeartbeatHolder.refresh(sn);

        // 2. 这里的判断条件要放宽：如果是 null 或者当前是离线，都要触发一次数据库同步
        Device device = deviceRepository.findByDeviceSn(sn);
        if (device != null) {
            if (device.getIsOnline() == null || !device.getIsOnline() || device.getLastSeen() == null) {
                device.setIsOnline(true);
                device.setActive(true);
                device.setLastSeen(LocalDateTime.now());
                deviceRepository.save(device);
                log.info("设备 {} 状态初始化/重连成功，已同步至数据库", sn);
            }
        }
    }

}
