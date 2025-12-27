package com.ray.atten.middle.module.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrintPayloadDto implements Serializable {

    private CardTemplateResponseDto cardTemplate;

    private List<OaEmployeeDto> employeeList;
}
