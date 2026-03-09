package com.ray.atten.middle.module.dto;

import com.ray.atten.middle.module.model.Company;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class CompanyDto extends BaseDto implements Serializable {

    private String name;

    public static CompanyDto convertTo(Company company) {
        if (company == null) {
            return null;
        }
        CompanyDto dto = new CompanyDto();
        dto.setName(company.getName());
        dto.setUuid(company.getUuid());
        dto.setCreateTime(company.getCreateTime());
        return dto;
    }

    public static List<CompanyDto> convertList(List<Company> companies) {
        return companies.stream().map(CompanyDto::convertTo).collect(Collectors.toList());
    }
}
