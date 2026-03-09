package com.ray.atten.middle.module.repository;

import com.ray.atten.middle.module.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    // 根據 uuid 查詢公司
    Optional<Company> findByUuid(UUID uuid);

    // 根據名稱查詢（用於重複校驗）
    Optional<Company> findByName(String name);

    // 根據 uuid 列表批量查詢
    List<Company> findAllByUuidIn(List<UUID> uuids);

}