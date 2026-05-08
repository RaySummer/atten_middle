package com.ray.atten.middle.module.repository;

import com.ray.atten.middle.module.model.EmployeeSyncQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public interface EmployeeSyncQueueRepository extends JpaRepository<EmployeeSyncQueue, Long> {

    // 查询所有状态为 0 (待同步) 的记录
    List<EmployeeSyncQueue> findByStatus(Integer status);

    @Query(nativeQuery = true, value = "select * from employee_sync_queue where pin = :pin")
    EmployeeSyncQueue findByPin(String pin);

    @Query(nativeQuery = true, value = "select * from employee_sync_queue where pin = :pin and fid = :fid")
    EmployeeSyncQueue findByPinAndFid(String pin, Integer fid);

    @Query(nativeQuery = true, value = "select * from employee_sync_queue where pin = :pin and type = :type")
    EmployeeSyncQueue findByPinAndType(String pin, String type);

    /**
     * 根据 PIN 列表查询对应的同步队列数据
     *
     * @param pins 员工工号列表
     * @return 匹配的 EmployeeSyncQueue 列表
     */
    List<EmployeeSyncQueue> findByPinIn(List<String> pins);
}
