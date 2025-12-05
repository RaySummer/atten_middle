package com.ray.atten.middle.module.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Access(AccessType.FIELD)
@Entity
@Table(name = "device_commands")
public class DeviceCommand {
    // 指令ID (主键)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 目标设备序列号
    @Column(nullable = false)
    private String deviceSn;

    // 指令内容 (例如: GETUSER, REBOOT)
    @Column(nullable = false)
    private String commandContent;

    // 指令状态: 0=待发送(PENDING), 1=已发送(SENT), 2=已执行(EXECUTED)
    // 默认状态为 0 (待发送)
    private Integer status = 0;

    // 创建时间
    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 在数据持久化之前，自动设置创建时间
     */
    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
