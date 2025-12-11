package com.ray.atten.middle.module.repository;

import com.ray.atten.middle.module.model.OaEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface OaEmployeeRepository extends JpaRepository<OaEmployee, Long>, JpaSpecificationExecutor<OaEmployee> {

    /**
     * 根據給定的 PIN 碼集合查詢所有匹配的員工。
     * Spring Data JPA 會自動實現這個方法，性能較高。
     */
    List<OaEmployee> findByPinIn(Set<String> pins);
}
