package com.ray.atten.middle.module.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class DeviceCommandDto extends BaseDto implements Serializable {

    private String deviceSn;

}
