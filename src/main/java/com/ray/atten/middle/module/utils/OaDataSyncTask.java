package com.ray.atten.middle.module.utils;

import com.ray.atten.middle.module.dto.OaEmployeeDto;
import com.ray.atten.middle.module.model.Company;
import com.ray.atten.middle.module.repository.CompanyRepository;
import com.ray.atten.middle.module.service.OaEmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class OaDataSyncTask implements CommandLineRunner {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private OaEmployeeService oaService; // 假设这是你调用 OA 的 Service

    @Override
    public void run(String... args) {
        syncCompaniesFromOa();
    }

    /**
     * 每 30 分鐘執行一次同步 (1800000 毫秒)
     * 使用 fixedRate 確保每隔半小時觸發一次
     */
    @Scheduled(fixedRate = 1800000)
    public void syncCompaniesFromOa() {
        log.info("开始从 OA 同步公司维度数据...");
        try {
            // 1. 从 OA 获取所有员工数据
            List<OaEmployeeDto> allEmployees = oaService.findAllEmployee();

            // 2. 提取所有唯一的公司名称
            Set<String> oaCompanyNames = allEmployees.stream()
                    .map(OaEmployeeDto::getCompany)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .collect(Collectors.toSet());

            // 3. 获取本地数据库已有的公司
            List<Company> localCompanies = companyRepository.findAll();
            Set<String> localNames = localCompanies.stream()
                    .map(Company::getName)
                    .collect(Collectors.toSet());

            // 4. 找出需要新增的公司
            List<Company> newCompanies = oaCompanyNames.stream()
                    .filter(name -> !localNames.contains(name))
                    .map(name -> {
                        Company c = new Company();
                        c.setName(name);
                        c.setUuid(UUID.randomUUID()); // 生成唯一标识
                        return c;
                    })
                    .collect(Collectors.toList());

            if (!newCompanies.isEmpty()) {
                companyRepository.saveAll(newCompanies);
                log.info("同步完成，新增了 {} 家公司: {}", newCompanies.size(),
                        newCompanies.stream().map(Company::getName).collect(Collectors.toList()));
            } else {
                log.info("同步完成，公司维度无变化。");
            }

        } catch (Exception e) {
            log.error("从 OA 同步公司数据失败: ", e);
        }
    }
}