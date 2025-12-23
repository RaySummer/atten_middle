package com.ray.atten.middle.module.repository;

import com.ray.atten.middle.module.model.SysConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SysConfigRepository extends JpaRepository<SysConfig, Long> {

    SysConfig findByConfigKey(String key);
}
