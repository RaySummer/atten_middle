package com.ray.atten.middle.module.repository;

import com.ray.atten.middle.module.model.DeviceCommand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public interface DeviceCommandRepository extends JpaRepository<DeviceCommand, Long> {

    /**
     * 查找某个设备所有状态为 PENDING (0) 的指令
     */
    List<DeviceCommand> findByDeviceSnAndStatus(String deviceSn, Integer status);

//    List<DeviceCommand> findByDeviceSnAndExecutedFalseOrderByCreateTimeAsc(String sn);

    /**
     * 更新指定ID指令的状态
     */
    @Modifying
    @Query("UPDATE DeviceCommand dc SET dc.status = :newStatus WHERE dc.id = :commandId")
    void updateCommandStatus(@Param("commandId") Long commandId, @Param("newStatus") Integer newStatus);


    /**
     * 查找指定设备、指定内容、状态为 SENT (1) 的指令
     * 我们只关心最近发送的那个
     */
    List<DeviceCommand> findByDeviceSnAndCommandContentAndStatusOrderByCreateTimeDesc(
            String deviceSn, String commandContent, Integer status);
}
