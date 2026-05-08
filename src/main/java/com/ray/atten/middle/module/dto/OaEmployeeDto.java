package com.ray.atten.middle.module.dto;

import com.ray.atten.middle.module.model.EmployeeSyncQueue;
import com.ray.atten.middle.module.model.OaEmployee;
import lombok.*;

import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

    private String photoBase64;

    private List<EmployeeSyncDto> syncList = new ArrayList<>();

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

        List<EmployeeSyncDto> employeeSyncDtoList = convertSyncData(emp.getSyncQueue());
        if (!employeeSyncDtoList.isEmpty()) {
            dto.getSyncList().addAll(convertSyncData(emp.getSyncQueue()));
        }
        return dto;
    }

    private static List<EmployeeSyncDto> convertSyncData(Set<EmployeeSyncQueue> queues) {
        if (queues == null || queues.isEmpty()) {
            return new ArrayList<>();
        }
        List<EmployeeSyncDto> list = new ArrayList<>();
        for (EmployeeSyncQueue que :
                queues) {
            EmployeeSyncDto dto = new EmployeeSyncDto();
            dto.setBase64Data(que.getBase64Data());
            dto.setFid(que.getFid());
            dto.setPin(que.getPin());
            dto.setType(que.getType());
            dto.setUuid(que.getUuid());

            list.add(dto);
        }
        return list;
    }

}
