package com.ray.atten.middle.module.dto;

import lombok.*;

import java.io.Serializable;
import java.util.List;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceGroupDto implements Serializable {

    private Long id;

    private String groupName;

    private List<String> deviceSns;
}
