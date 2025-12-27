package com.ray.atten.middle.module.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Access(AccessType.FIELD)
@Entity
@Table(name = "device_commands")
public class DeviceCommand extends BaseEntity implements Serializable {

    // 目标设备序列号
    @Column(nullable = false)
    private String deviceSn;

    // 指令内容 (例如: GETUSER, REBOOT)
    @Column(columnDefinition = "TEXT")
    private String commandContent;

    // 指令状态: 0=待发送(PENDING), 1=已发送(SENT), 2=已执行(EXECUTED)
    // 默认状态为 0 (待发送)
    private Integer status = 0;

}
