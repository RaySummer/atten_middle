package com.ray.atten.middle.module.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Access(AccessType.FIELD)
@Entity
@Table(name = "attendance_group")
public class AttendanceGroup extends BaseEntity implements Serializable {

    @Column(unique = true, nullable = false)
    private String groupName;

    // 【多對多關係配置】
    // JoinTable: 定義中間表 group_device_mapping
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "group_device_mapping",
            // JoinColumns: 定義當前實體 (AttendanceGroup) 在中間表中的外鍵列名
            joinColumns = @JoinColumn(name = "group_id"),
            // inverseJoinColumns: 定義對面實體 (Device) 在中間表中的外鍵列名
            inverseJoinColumns = @JoinColumn(name = "device_id")
    )
    private Set<Device> devices = new HashSet<>();

}
