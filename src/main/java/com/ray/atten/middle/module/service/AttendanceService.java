package com.ray.atten.middle.module.service;

import com.ray.atten.middle.module.model.AttendanceLog;
import com.ray.atten.middle.module.repository.AttendanceLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AttendanceService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private AttendanceLogRepository logRepo;

    @Transactional
    public List<AttendanceLog> processAttLogData(String sn, String bodyData) {
        log.debug("Processing ATTLOG data from: " + sn);
        List<AttendanceLog> logs = new ArrayList<>();

        // 1. 按行分割数据
        String[] lines = bodyData.trim().split("\n");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        for (String line : lines) {
            if (line.trim().isEmpty()) continue;

            // 数据是 Tab 分隔的：
            // 101021	2025-11-22 12:01:16	0	15	0	0	0	255	0	0
            // verifyType = 15(人脸) 0(指静脉或人脸或指纹或卡或密码) 1(仅指纹)
            // STATUS:0-成功，1-失败，2-黑名单
            String[] fields = line.split("\t");

            if (fields.length >= 4) { // 至少应有 PIN, Time, VerifyType, Status
                String verifyTime = fields[1].trim();
                LocalDateTime verifiedTime = LocalDateTime.parse(verifyTime, FORMATTER);
                AttendanceLog attendanceLog = logRepo.findByDeviceSnAndUserPinAndVerifyTime(sn, fields[0].trim(), verifiedTime);
                if (attendanceLog == null) {
                    attendanceLog = new AttendanceLog();
                    attendanceLog.setDeviceSn(sn);
                    attendanceLog.setUserPin(fields[0].trim());

                    attendanceLog.setVerifyTime(verifiedTime);
                    attendanceLog.setStatus(Integer.valueOf(fields[2].trim()));
                    attendanceLog.setVerifyType(Integer.valueOf(fields[3].trim()));

                    logRepo.save(attendanceLog);
                    logs.add(attendanceLog);
                    log.debug("Saved AttLog: " + attendanceLog.getUserPin() + " " + attendanceLog.getVerifyTime());
                }
                log.debug("This AttLog Exists " + attendanceLog.getUserPin() + " " + attendanceLog.getVerifyTime());
            }
        }

        return logs;
    }

}
