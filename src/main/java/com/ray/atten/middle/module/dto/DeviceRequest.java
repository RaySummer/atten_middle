package com.ray.atten.middle.module.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class DeviceRequest extends BaseDto implements Serializable {

    // 唯一标识：序列号
    @NotBlank(message = "设备序列号不能为空")
    @Size(max = 50)
    private String deviceSn;

    private String alias;    // 考勤机名称
    private String location; // 考勤机位置
    private String model;    // 考勤机型号
    private String ipAddress; // 考勤机IP
    private Boolean active;  // 是否激活 (true/false)
    private List<UUID> companyUuids;
}
