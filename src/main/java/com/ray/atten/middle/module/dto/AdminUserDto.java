package com.ray.atten.middle.module.dto;

import com.ray.atten.middle.module.model.AdminUser;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class AdminUserDto extends BaseDto implements Serializable {

    private String username;

    private List<CompanyDto> managedCompanies;

    private boolean isSuperAdmin;

    public static AdminUserDto convertTo(AdminUser admin) {
        if (admin == null) {
            return null;
        }
        AdminUserDto dto = new AdminUserDto();
        dto.setUsername(admin.getUsername());
        dto.setSuperAdmin(admin.getIsSuperAdmin());
        dto.setUuid(admin.getUuid());
        if (!admin.getManagedCompanies().isEmpty()) {
            dto.setManagedCompanies(CompanyDto.convertList(new ArrayList<>(admin.getManagedCompanies())));
        }
        dto.setCreateTime(admin.getCreateTime());
        return dto;
    }

    public static List<AdminUserDto> convertList(List<AdminUser> adminUsers) {
        return adminUsers.stream().map(AdminUserDto::convertTo).collect(Collectors.toList());
    }
}
