package com.ray.atten.middle.module.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceLogDto extends BaseDto implements Serializable {
    private String userPin;
    private String userName;   // 从 OaEmployee 关联获取
    private String deviceSn;
    private LocalDateTime verifyTime;
    private Integer status;
    private Integer verifyType;
}
