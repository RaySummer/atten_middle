package com.ray.atten.middle.module.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class IClockAsyncService {

    // 注入您的業務服務
    @Autowired
    private AttendanceService attendanceService;
    @Autowired
    private EmployeeService employeeService; // 假設處理 BIODATA 的服務

    private static final String DEFAULT_ENCODING = "GBK"; // 考勤機常用編碼

    /**
     * 專門用於處理考勤機數據的異步方法。
     *
     * @param table      數據表類型
     * @param dataBuffer 原始數據字節數組
     * @param dataLength 實際讀取的字節長度
     * @Async 默認使用 SimpleAsyncTaskExecutor，建議在 AsyncConfig 中定義專門的線程池。
     * * @param sn 設備序列號
     */
    @Async("iClockExecutor") // 假設您在 AsyncConfig 中定義了一個名為 iClockExecutor 的線程池
    public void processIClockDataAsync(String sn, String table, byte[] dataBuffer, int dataLength) {

        if (!StringUtils.hasText(table)) {
            // 數據不完整，不處理
            return;
        }

        String bodyData = "";
        try {
            // 1. 執行字符集轉換 (耗時操作)
            // 根據您的描述，這裡應該嘗試 GBK
            bodyData = new String(dataBuffer, 0, dataLength, DEFAULT_ENCODING);

            process(table, sn, bodyData);

        } catch (UnsupportedEncodingException e) {
            log.debug("字符集轉換失敗，嘗試 UTF-8。");
            // 如果 GBK 失敗，嘗試 UTF-8
            bodyData = new String(dataBuffer, 0, dataLength, StandardCharsets.UTF_8);
            process(table, sn, bodyData);
        } catch (Exception e) {
            log.debug("異步處理考勤數據時發生業務異常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void process(String table, String sn, String bodyData) {
        // 2. 業務邏輯判斷和調用 (耗時操作)
        if ("USERINFO".equalsIgnoreCase(table)) {
            // deviceSyncService.processUserInfo(sn, bodyData);
            log.debug("--- 異步處理 USERINFO 數據... ---");
        } else if ("ATTLOG".equalsIgnoreCase(table)) {
            // 這裡調用您原來的服務方法
            attendanceService.processAttLogData(sn, bodyData);
            log.debug("--- 異步收到設備[" + sn + "] 考勤記錄並保存... ---");
        } else if ("BIODATA".equalsIgnoreCase(table)) {
            employeeService.processBioData(sn, bodyData);
            log.debug("--- 異步保存 BIODATA 數據... ---");
        } else {
            log.debug("--- 異步處理其他類型數據: " + table + " ---");
        }
    }
}
