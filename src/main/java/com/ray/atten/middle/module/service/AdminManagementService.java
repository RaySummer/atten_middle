package com.ray.atten.middle.module.service;

import com.ray.atten.middle.module.dto.AdminCreateRequest;
import com.ray.atten.middle.module.dto.AdminUserDto;
import com.ray.atten.middle.module.dto.BaseDto;
import com.ray.atten.middle.module.dto.CompanyDto;
import com.ray.atten.middle.module.model.AdminUser;
import com.ray.atten.middle.module.model.Company;
import com.ray.atten.middle.module.repository.AdminUserRepository;
import com.ray.atten.middle.module.repository.CompanyRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminManagementService {

    @Autowired
    private AdminUserRepository adminUserRepository;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 創建新管理員並分配公司
     */
    @Transactional
    public void createAdmin(AdminCreateRequest request) {
        if (adminUserRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("用戶名已存在");
        }

        AdminUser newUser = new AdminUser();
        newUser.setUsername(request.getUsername());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setIsSuperAdmin(false);
        // uuid 建議在 Entity 的 @PrePersist 中自動生成，或者在這裡手動 set
        // newUser.setUuid(UUID.randomUUID().toString());

        // 使用傳入的公司 UUID 列表進行關聯
        if (request.getManagedCompanies() != null && !request.getManagedCompanies().isEmpty()) {
            List<Company> companies = companyRepository.findAllByUuidIn(request.getManagedCompanies().stream().map(BaseDto::getUuid).collect(Collectors.toList()));
            newUser.setManagedCompanies(new HashSet<>(companies));
        }
        adminUserRepository.save(newUser);
    }

    /**
     * 更新管理員的公司管轄權限
     */
    @Transactional
    public void updateAdminPermissions(UUID uuid, AdminCreateRequest request) {
        AdminUser admin = adminUserRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("管理員不存在"));

        if (admin.getIsSuperAdmin()) {
            throw new RuntimeException("不能修改超級管理員的權限");
        }

        //修改密码
        if (StringUtils.isNotEmpty(request.getPassword())) {
            admin.setPassword(passwordEncoder.encode(request.getPassword()));
            adminUserRepository.save(admin);
        }

        List<Company> companies = companyRepository.findAllByUuidIn(request.getManagedCompanies().stream().map(BaseDto::getUuid).collect(Collectors.toList()));
        admin.setManagedCompanies(new HashSet<>(companies));
        adminUserRepository.save(admin);
    }

    @Transactional
    public Company createCompany(String companyName) {
        if (companyRepository.findByName(companyName).isPresent()) {
            throw new RuntimeException("公司名稱已重複");
        }
        Company company = new Company();
        company.setName(companyName);
        return companyRepository.save(company);
    }

    public List<AdminUserDto> getAllAdmin() {
        return AdminUserDto.convertList(adminUserRepository.findAll());
    }

    /**
     * 獲取所有公司清單 (超管專用)
     */
    public List<CompanyDto> getAllCompanies() {
        return CompanyDto.convertList(companyRepository.findAll());
    }

    /**
     * 刪除公司
     */
    @Transactional
    public void deleteCompany(UUID uuid) {
        Company company = companyRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("公司不存在"));

        // 檢查是否有管理員關聯（可選）
        companyRepository.delete(company);
    }

    /**
     * 獲取單個公司詳情 (根據 UUID)
     */
    public Company getCompanyByUuid(UUID uuid) {
        return companyRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("公司不存在"));
    }

    @Transactional
    public void deleteAdmin(UUID adminUuid) {
        AdminUser admin = adminUserRepository.findByUuid(adminUuid)
                .orElseThrow(() -> new RuntimeException("管理员不存在"));
        if (admin.getIsSuperAdmin()) {
            throw new RuntimeException("无法删除超级管理员");
        }
        adminUserRepository.delete(admin);
    }
}
