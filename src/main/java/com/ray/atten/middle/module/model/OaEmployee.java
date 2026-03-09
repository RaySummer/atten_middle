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

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Access(AccessType.FIELD)
@Entity
@Table(name = "oa_employee")
// 定义过滤器：名称为 companyFilter，接受一个名为 names 的字符串列表参数
@FilterDef(name = SecurityConstants.COMPANY_FILTER_NAME, parameters = @ParamDef(name = SecurityConstants.COMPANY_PARAM_NAME, type = "string"))
// 定义过滤逻辑：要求字段 company_name 在参数列表 :names 中
@Filter(name = SecurityConstants.COMPANY_FILTER_NAME, condition = "company IN (:" + SecurityConstants.COMPANY_PARAM_NAME + ")")
public class OaEmployee extends BaseEntity implements Serializable {

    private String avatar;      //员工头像
    // 核心信息
    private String pin;         // 員工工號 (PIN)
    private String name;        // 員工姓名

    @Column(name = "company")
    private String company;     // 所屬分公司/機構
    private String dept;        // 所屬部門
    private String post;        // 职位

    // 狀態和時間
    private Boolean inService;  // 是否在職 (true/false)

    private String officeLocation; //办公地点

    private LocalDateTime entryDate; // 入職時間

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pin", referencedColumnName = "pin", insertable = false, updatable = false)
    private EmployeeSyncQueue syncQueue;

}
