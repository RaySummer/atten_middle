package com.ray.atten.middle.module.model;

import com.ray.atten.middle.module.utils.SecurityConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

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
// 1. 定义过滤器：参数类型改为 string，因为 UUID 在 Java 中通常以 String 形式处理
@FilterDef(
        name = SecurityConstants.COMPANY_FILTER_NAME,
        parameters = @ParamDef(name = SecurityConstants.COMPANY_PARAM_NAME, type = "string")
)
// 2. 核心修改：通过中间表 device_companies 关联 companies 表的 uuid 字段进行过滤
@Filter(
        name = SecurityConstants.COMPANY_FILTER_NAME,
        condition = "id IN (SELECT dc.device_id FROM device_companies dc " +
                "JOIN sys_company sc ON dc.company_id = sc.id " +
                "WHERE sc.uuid IN (:" + SecurityConstants.COMPANY_PARAM_NAME + "))"
)
public class Device extends BaseEntity implements Serializable {

    @Column(unique = true, nullable = false)
    private String deviceSn;

    private String alias;

    private String location;

    private String model;

    private String ipAddress;

    private Boolean active = Boolean.TRUE;

    private Boolean isOnline = Boolean.TRUE;

    private LocalDateTime lastSeen;

    // 3. 多对多关联：指向公司实体
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "device_companies",
            joinColumns = @JoinColumn(name = "device_id"),
            inverseJoinColumns = @JoinColumn(name = "company_id")
    )
    private Set<Company> companies = new HashSet<>();

    @ManyToMany(mappedBy = "devices", fetch = FetchType.LAZY)
    private Set<AttendanceGroup> groups = new HashSet<>();

}