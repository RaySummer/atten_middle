package com.ray.atten.middle.module.dto;

import lombok.Data;

@Data
public class EmployeeFingerFidDto {

    private Integer fid;

    private String base64Data;

    private Integer base64Size;

    private String type;

}
