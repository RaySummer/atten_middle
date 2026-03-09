package com.ray.atten.middle.module.repository;

import com.ray.atten.middle.module.model.OaEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface OaEmployeeRepository extends JpaRepository<OaEmployee, Long>, JpaSpecificationExecutor<OaEmployee> {

    /**
     * 根據給定的 PIN 碼集合查詢所有匹配的員工。
     * Spring Data JPA 會自動實現這個方法，性能較高。
     */
    List<OaEmployee> findByPinIn(Set<String> pins);

    List<OaEmployee> findAllByUuidIn(List<UUID> ids);

    // 虽然有 Aspect，但定义这个方法可以增加代码的可读性
    List<OaEmployee> findByCompanyIn(List<String> companies);

    // 根据 PIN 查找员工（通常用于流水归因，不受权限过滤影响）
    Optional<OaEmployee> findByPin(String pin);
}
