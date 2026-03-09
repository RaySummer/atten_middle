package com.ray.atten.middle.module.model;

import com.ray.atten.middle.module.utils.SecurityConstants;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "attendance_group")
@Data
// 统一定义过滤器
@FilterDef(name = "companyFilter", parameters = @ParamDef(name = "names", type = "string"))
// 核心逻辑：查询所有关联了“属于指定分公司设备”的考勤组
@Filter(
        name = "companyFilter",
        condition = "id IN (" +
                "  SELECT DISTINCT gdm.group_id " +
                "  FROM group_device_mapping gdm " +
                "  JOIN devices d ON gdm.device_id = d.id " +
                "  WHERE d.company_name IN (:names)" +
                ")"
)
public class AttendanceGroup extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String groupName;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "group_device_mapping",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "device_id")
    )
    private Set<Device> devices = new HashSet<>();
}
