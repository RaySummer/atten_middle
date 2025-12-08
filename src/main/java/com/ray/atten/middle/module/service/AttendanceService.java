package com.ray.atten.middle.module.service;

import com.ray.atten.middle.module.model.AttendanceLog;
import com.ray.atten.middle.module.model.OperationLog;
import com.ray.atten.middle.module.repository.AttendanceLogRepository;
import com.ray.atten.middle.module.repository.OperationLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    @Autowired
    private DeviceConfigService deviceConfigService;

    @Autowired
    private OperationLogRepository operationLogRepository;

    /**
     * 处理并保存考勤记录
     *
     * @param deviceSn           上传数据的设备序列号
     * @param attendanceDataList 考勤记录列表，格式为 "PIN\tTime\tVerifyType\tStatus"
     */
    public void processAndSaveAttendance(String deviceSn, List<String> attendanceDataList) {
        if (attendanceDataList == null || attendanceDataList.isEmpty()) {
            return;
        }

        log.debug("DEBUG: 已从设备 " + deviceSn + " 接收到 " + attendanceDataList.size() + " 条考勤记录。");

        int count = 0;
        for (String record : attendanceDataList) {
            if (record.trim().isEmpty()) continue;

            // 数据格式示例: 1001\t2023-12-01 09:00:00\t0\t15
            String[] parts = record.split("\t");
            if (parts.length >= 2) {
                try {
                    AttendanceLog log = new AttendanceLog();
                    log.setDeviceSn(deviceSn);
                    log.setUserPin(parts[0]);

                    // ZK通常发送 yyyy-MM-dd HH:mm:ss
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    log.setVerifyTime(LocalDateTime.parse(parts[1], formatter));

                    log.setStatus(parts[2]);
                    log.setVerifyType(parts[3]);

                    logRepo.save(log);
                    count++;
                } catch (Exception e) {
                    System.err.println("解析行失败: " + count + " 错误: " + e.getMessage());
                }
            }
        }
    }

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
                AttendanceLog attendanceLog = logRepo.findByDeviceSnAndUserPinAndVerifyTime();
                if (attendanceLog == null) {
                    attendanceLog = new AttendanceLog();
                    attendanceLog.setDeviceSn(sn);
                    attendanceLog.setUserPin(fields[0].trim());
                    String verifyTime = fields[1].trim();
                    LocalDateTime verifiedTime = LocalDateTime.parse(verifyTime, FORMATTER);
                    attendanceLog.setVerifyTime(verifiedTime);
                    attendanceLog.setStatus(fields[2].trim());
                    attendanceLog.setVerifyType(fields[3].trim());

                    logRepo.save(attendanceLog);
                    logs.add(attendanceLog);
                    log.debug("Saved AttLog: " + attendanceLog.getUserPin() + " " + attendanceLog.getVerifyTime());
                }
                log.debug("This AttLog Exists " + attendanceLog.getUserPin() + " " + attendanceLog.getVerifyTime());
            }
        }

//        try {
//            if (!logs.isEmpty()) {
//                OperationLog operationLog = new OperationLog();
//                AttendanceLog attendanceLog = logs.get(logs.size() - 1);
//                operationLog.setDeviceSn(sn);
//                operationLog.setOperationTime(attendanceLog.getVerifyTime());
//                operationLog.setOperation(1);
//
//                operationLogRepository.save(operationLog);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            log.error(" The Operation save error");
//        }

        return logs;
    }

}
