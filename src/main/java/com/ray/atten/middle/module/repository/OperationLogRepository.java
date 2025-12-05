package com.ray.atten.middle.module.repository;

import com.ray.atten.middle.module.model.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    @Query(nativeQuery = true, value = "select * from operation_log where operation = :operation and deviceSn = :sn order by create_time desc LIMIT 1")
    OperationLog findLastTimeByOperation(Integer operation, String sn);
}
