package com.ray.atten.middle.module.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OaEmployeeDto implements Serializable {

    private Long id;
    private String pin;         // 員工工號 (PIN)
    private String name;        // 員工姓名
    private String company;     // 所屬分公司/機構
    private String dept;        // 所屬部門
    private Boolean inService;  // 是否在職 (true/false)
    private LocalDateTime entryDate; // 入職時間
    private LocalDateTime createTime; // 創建時間
    private String fingerprint;
    private String photoBase64;
    //手指编号，取值为0到9
    private Integer fid;
    //指纹模版二进制数据经过base64编码之后的长度
    private Integer fingerSize;
    private Integer photoSize;

}
