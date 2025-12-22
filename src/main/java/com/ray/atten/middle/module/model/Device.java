package com.ray.atten.middle.module.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Access(AccessType.FIELD)
@Entity
@Table(name = "devices")
public class Device implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 考勤机序列号 (唯一标识)
    @Column(unique = true, nullable = false)
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
    private Boolean active = Boolean.TRUE;

    //物理状态（设备当前是否连通）
    private Boolean isOnline = Boolean.TRUE;

    //最后一次心跳时间
    private LocalDateTime lastSeen;

    // 创建时间
    private LocalDateTime createTime;

    // 修改时间
    private LocalDateTime updateTime;

    // 【多對多關係配置】
    // mappedBy: 指向 AttendanceGroup 實體中的 'devices' 屬性，表示 Device 是關係的從屬方
    @ManyToMany(mappedBy = "devices", fetch = FetchType.LAZY)
    private Set<AttendanceGroup> groups = new HashSet<>();

    // --- 自动维护时间戳 ---

    @PrePersist
    protected void onCreate() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
