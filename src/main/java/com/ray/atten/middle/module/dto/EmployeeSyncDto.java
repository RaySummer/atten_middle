package com.ray.atten.middle.module.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class EmployeeSyncDto extends BaseDto implements Serializable {

    // 工号
    private String pin;


    private String base64Data;

    //手指编号，取值为0到9
    private Integer fid;

    //base64编码长度
    private Integer base64Size;

    //指纹   照片
    private String type;

}
