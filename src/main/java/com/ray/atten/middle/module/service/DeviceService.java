package com.ray.atten.middle.module.service;

import com.ray.atten.middle.module.dto.DeviceDto;
import com.ray.atten.middle.module.dto.DeviceRequest;
import com.ray.atten.middle.module.model.Company;
import com.ray.atten.middle.module.model.Device;
import com.ray.atten.middle.module.repository.CompanyRepository;
import com.ray.atten.middle.module.repository.DeviceRepository;
import com.ray.atten.middle.module.service.base.DataFilterService;
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
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeviceService {

    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private DataFilterService dataFilterService;
    @Autowired
    private CompanyRepository companyRepository;

    /**
     * 新增或更新考勤机信息
     *
     * @param request 前端传入的设备信息
     * @return 保存后的 Device 实体
     */
    @Transactional // 必须开启事务，保证多对多关联表的原子性
    public DeviceDto saveOrUpdateDevice(DeviceRequest request) {
        Device device = null;
        if (StringUtils.isNotEmpty(request.getDeviceSn())) {
            device = deviceRepository.findByDeviceSn(request.getDeviceSn());
        }

        if (device == null) {
            device = new Device();
            device.setDeviceSn(request.getDeviceSn()); // 仅在新增时设置 SN
        }

        // --- 多对多关联处理 ---
        if (request.getCompanyUuids() != null) {
            // 如果传入了列表（即使是空列表），执行更新
            List<Company> companyList = companyRepository.findAllByUuidIn(request.getCompanyUuids());
            // 重新设置关联（Hibernate 会自动处理中间表的 DELETE 和 INSERT）
            device.setCompanies(new HashSet<>(companyList));
        } else {
            // 如果 request 里的 companyUuids 是 null，视业务需求决定是否清空
            // 通常建议如果前端传了空数组，则清空；如果没传该字段，则保持现状
        }

        // --- 更新其他字段 ---
        device.setAlias(request.getAlias());
        device.setLocation(request.getLocation());
        device.setModel(request.getModel());
        device.setIpAddress(request.getIpAddress());
        device.setActive(request.getActive());

        // 执行保存
        Device savedDevice = deviceRepository.save(device);
        return DeviceDto.convertToDto(savedDevice);
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
        List<DeviceDto> collect = entityList.stream().map(DeviceDto::convertToDto).collect(Collectors.toList());
        return collect;
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
