package com.ray.atten.middle.module.repository;

import com.ray.atten.middle.module.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long>, JpaSpecificationExecutor<Device> {

    // 根据序列号查找设备，用于判断是新增还是更新
    Device findByDeviceSn(String deviceSn);

    List<Device> findByActiveTrue();

    /**
     * 【新增】根據一組設備序列號批量查詢設備實體
     */
    List<Device> findByDeviceSnIn(List<String> sns);

    @Modifying
    @Transactional // 确保更新操作在事务中执行
    @Query("UPDATE Device d SET d.active = :active WHERE d.deviceSn IN :sns")
    void batchUpdateActiveStatus(@Param("sns") List<String> sns, @Param("active") Boolean active);

    @Modifying
    @Transactional
    @Query("UPDATE Device d SET d.active = false, d.isOnline = false WHERE d.deviceSn IN :sns")
    void batchSetOffline(@Param("sns") List<String> sns);

    // 单个设备上线更新
    @Modifying
    @Transactional
    @Query("UPDATE Device d SET d.isOnline = true, d.lastSeen = :now WHERE d.deviceSn = :sn")
    void updateOnlineStatus(@Param("sn") String sn, @Param("now") LocalDateTime now);

//    List<Device> findByCompanies_UuidIn(List<String> companyNames);

}
