package com.ray.atten.middle.module.repository;

import com.ray.atten.middle.module.model.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LogRepository extends JpaRepository<LogEntry, Long>, JpaSpecificationExecutor<LogEntry> {

}
