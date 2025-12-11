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
@Table(name = "oa_employee")
public class OaEmployee {

    // 主鍵
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 核心信息
    private String pin;         // 員工工號 (PIN)
    private String name;        // 員工姓名

    // 組織結構 (未來級聯查詢的基礎)
    private String company;     // 所屬分公司/機構
    private String dept;        // 所屬部門

    // 狀態和時間
    private Boolean inService;  // 是否在職 (true/false)
    private LocalDateTime entryDate; // 入職時間
    private LocalDateTime createTime; // 創建時間
    private LocalDateTime updateTime; // 修改時間

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
