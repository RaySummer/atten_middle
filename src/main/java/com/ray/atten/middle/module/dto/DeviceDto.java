package com.ray.atten.middle.module.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ray.atten.middle.module.model.Company;
import com.ray.atten.middle.module.model.Device;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceDto extends BaseDto implements Serializable {

    private String deviceSn;

    // 考勤机名称/别名
    private String alias;

    // 考勤机位置
    private String location;

    // 考勤机型号
    private String model;

    // 考勤机IP
    private String ipAddress;

    // 是否激活/启用同步
    private Boolean active;

    //物理状态（设备当前是否连通）
    private Boolean isOnline;

    private List<UUID> companyUuids;

    //最后一次心跳时间
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime lastSeen;

    public static DeviceDto convertToDto(Device device) {
        if (device == null) {
            return null;
        }
        DeviceDto dto = new DeviceDto();
        dto.setUuid(device.getUuid());
        dto.setDeviceSn(device.getDeviceSn());
        dto.setAlias(device.getAlias());
        dto.setLocation(device.getLocation());
        dto.setModel(device.getModel());
        dto.setIpAddress(device.getIpAddress());
        dto.setActive(device.getActive());
        dto.setCreateTime(device.getCreateTime());
        dto.setUpdateTime(device.getUpdateTime());
        dto.setIsOnline(device.getIsOnline());
        dto.setLastSeen(device.getLastSeen());

        if (device.getCompanies() != null) {
            // 将关联的 Company 对象的 UUID 提取出来给 DTO
            List<UUID> uuids = device.getCompanies().stream()
                    .map(Company::getUuid) // 假设 Company 类有 getUuid()
                    .collect(Collectors.toList());
            dto.setCompanyUuids(uuids);

        } else {
            dto.setCompanyUuids(new ArrayList<>());
        }

        return dto;
    }

    public static List<DeviceDto> convertToList(List<Device> list) {
        if (list.isEmpty()) {
            return null;
        }
        List<DeviceDto> dtoList = new ArrayList<>();
        for (Device de : list) {
            DeviceDto dto = convertToDto(de);
            dtoList.add(dto);
        }
        return dtoList;
    }

}
