package com.ray.atten.middle.module.repository;

import com.ray.atten.middle.module.model.AttendanceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, Long> {

    @Query(nativeQuery = true, value = "select * from attendance_logs where device_sn=:deviceSn and user_pin=:userPin and verify_time = :verifyTime")
    AttendanceLog findByDeviceSnAndUserPinAndVerifyTime(String deviceSn, String userPin, LocalDateTime verifyTime);
}
