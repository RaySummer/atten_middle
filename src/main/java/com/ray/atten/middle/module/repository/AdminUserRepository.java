package com.ray.atten.middle.module.repository;

import com.ray.atten.middle.module.model.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    // 根據 uuid 查詢管理員
    Optional<AdminUser> findByUuid(UUID uuid);

    // 根據用戶名查詢
    Optional<AdminUser> findByUsername(String username);
}