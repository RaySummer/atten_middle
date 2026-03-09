package com.ray.atten.middle.module.dto;

import lombok.Data;

import java.util.List;

@Data
public class AdminCreateRequest {

    private String username;

    private String password;

    private List<CompanyDto> managedCompanies;
}
