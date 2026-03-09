package com.ray.atten.middle.module.utils;

import com.ray.atten.middle.module.model.AdminUser;
import com.ray.atten.middle.module.model.Company;
import com.ray.atten.middle.module.repository.AdminUserRepository;
import com.ray.atten.middle.module.repository.CompanyRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AdminUserRepository adminUserRepository,
                           CompanyRepository companyRepository,
                           PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (companyRepository.count() == 0) {
            // 1. 初始化公司数据
            Company compA = createCompany("广东荻轩科技有限公司");
            Company compB = createCompany("广东金凯发日用品有限公司");
            Company compC = createCompany("广州齐家健康科技有限公司");
            Company compD = createCompany("广东金海纳实业有限公司");
            Company compE = createCompany("广州市锦尚精密制造有限公司");
            Company compF = createCompany("保盾（广州）安全防护科技有限公司");
            Company compG = createCompany("广东楠沃检测技术服务有限公司");
            Company compH = createCompany("广东金凯迪家居科技有限公司");
            Company compI = createCompany("广东金海纳健康科技集团有限公司");
            Company compJ = createCompany("广州金海纳防护用品有限公司");
            Company compK = createCompany("金海纳集团");
            Company compL = createCompany("广州市金凯迪家居用品有限公司");
            Company compM = createCompany("广东楠沃医疗科技有限公司");

            // 2. 初始化超级管理员 (不需要绑定公司，逻辑上全选)
            if (!adminUserRepository.findByUsername("superadmin").isPresent()) {
                AdminUser superAdmin = new AdminUser();
                superAdmin.setUsername("superadmin");
                superAdmin.setPassword(passwordEncoder.encode("superadmin@2026"));
                superAdmin.setIsSuperAdmin(true);
                adminUserRepository.save(superAdmin);
            }

            // 3. 初始化管理员
            if (!adminUserRepository.findByUsername("jhn").isPresent()) {
                AdminUser bjManager = new AdminUser();
                bjManager.setUsername("jhn");
                bjManager.setPassword(passwordEncoder.encode("jhn2026@admin"));
                bjManager.setIsSuperAdmin(false);

                Set<Company> managed = new HashSet<>();
                managed.add(compA);
                managed.add(compC);
                managed.add(compD);
                managed.add(compI);
                managed.add(compK);
                managed.add(compJ);
                bjManager.setManagedCompanies(managed);

                adminUserRepository.save(bjManager);
            }
            if (!adminUserRepository.findByUsername("jkd").isPresent()) {
                AdminUser bjManager = new AdminUser();
                bjManager.setUsername("jkd");
                bjManager.setPassword(passwordEncoder.encode("jkd2026@admin"));
                bjManager.setIsSuperAdmin(false);

                Set<Company> managed = new HashSet<>();
                managed.add(compB);
                managed.add(compL);
                managed.add(compH);
                managed.add(compM);
                bjManager.setManagedCompanies(managed);

                adminUserRepository.save(bjManager);
            }
            if (!adminUserRepository.findByUsername("xh").isPresent()) {
                AdminUser bjManager = new AdminUser();
                bjManager.setUsername("xh");
                bjManager.setPassword(passwordEncoder.encode("xh2026@admin"));
                bjManager.setIsSuperAdmin(false);

                Set<Company> managed = new HashSet<>();
                managed.add(compE);
                managed.add(compF);
                managed.add(compG);
                bjManager.setManagedCompanies(managed);

                adminUserRepository.save(bjManager);
            }
        }
    }

    private Company createCompany(String name) {
        Company c = new Company();
        c.setName(name);
        c.setUuid(UUID.randomUUID());
        return companyRepository.save(c);
    }
}