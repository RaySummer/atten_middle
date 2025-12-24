package com.ray.atten.middle.module.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OaEmployeeRequest implements Serializable {

    private String pin;         // 員工工號 (PIN)
    private String name;        // 員工姓名
    private String company;     // 所屬分公司/機構
    private String dept;        // 所屬部門
    private String inService;  // 是否在職 (true/false)
    private String officeLocation; //办公地点
    private LocalDateTime entryDate; // 入職時間

}
