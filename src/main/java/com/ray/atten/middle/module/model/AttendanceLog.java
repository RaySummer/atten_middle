package com.ray.atten.middle.module.model;

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

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Access(AccessType.FIELD)
@Entity
@Table(name = "attendance_logs")
@FilterDef(name = "companyFilter", parameters = @ParamDef(name = "names", type = "string"))
// 核心逻辑：user_pin 必须在 (属于这些公司的员工 PIN 集合) 之中
@Filter(
        name = "companyFilter",
        condition = "user_pin IN (SELECT e.pin FROM oa_employee e WHERE e.company IN (:names))"
)
public class AttendanceLog extends BaseEntity implements Serializable {


    private String userPin;    // 工号
    private String deviceSn;   // 设备序列号
    private LocalDateTime verifyTime; // 打卡时间
    private Integer status;    // 状态(0:上班, 1:下班等)
    private Integer verifyType;// 验证方式

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "userPin",
            referencedColumnName = "pin",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT) // 关键：禁止生成外键约束
    )
    private OaEmployee employee;

}
