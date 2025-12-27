package com.ray.atten.middle.module.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class EmployeeRequest extends BaseDto implements Serializable {

    private String pin;
    private String name;
    private String fingerprint;
    private String photo; // 前端传来的 Base64 字符串
    private String targetDeviceSn;
}
