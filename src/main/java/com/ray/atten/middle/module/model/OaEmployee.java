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
@Table(name = "oa_employee")
public class OaEmployee extends BaseEntity implements Serializable {

    private String avatar;      //员工头像
    // 核心信息
    private String pin;         // 員工工號 (PIN)
    private String name;        // 員工姓名

    // 組織結構 (未來級聯查詢的基礎)
    private String company;     // 所屬分公司/機構
    private String dept;        // 所屬部門
    private String post;        // 职位

    // 狀態和時間
    private Boolean inService;  // 是否在職 (true/false)

    private String officeLocation; //办公地点

    private LocalDateTime entryDate; // 入職時間

}
