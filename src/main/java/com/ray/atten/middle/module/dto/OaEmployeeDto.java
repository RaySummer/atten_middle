package com.ray.atten.middle.module.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ray.atten.middle.module.model.OaEmployee;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OaEmployeeDto extends BaseDto implements Serializable {

    private String pin;         // 員工工號 (PIN)
    private String avatar;
    private String name;        // 員工姓名
    private String company;     // 所屬分公司/機構
    private String dept;        // 所屬部門
    private String post;
    private Boolean inService;  // 是否在職 (true/false)
    private String officeLocation; //办公地点
//    @JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss")
    private LocalDateTime entryDate; // 入職時間
    private String fingerprint;
    private String photoBase64;
    //手指编号，取值为0到9
    private Integer fid;
    //指纹模版二进制数据经过base64编码之后的长度
    private Integer fingerSize;
    private Integer photoSize;

    public static OaEmployeeDto convertToDto(OaEmployee emp) {
        if (emp == null) {
            return null;
        }
        OaEmployeeDto dto = new OaEmployeeDto();
        dto.setUuid(emp.getUuid());
        dto.setPin(emp.getPin());
        dto.setAvatar(emp.getAvatar());
        dto.setName(emp.getName());
        dto.setCompany(emp.getCompany());
        dto.setDept(emp.getDept());
        dto.setPost(emp.getPost());
        dto.setInService(emp.getInService());
        dto.setOfficeLocation(emp.getOfficeLocation());
        dto.setEntryDate(emp.getEntryDate());
        dto.setCreateTime(emp.getCreateTime());
        return dto;
    }
}
