package com.ray.atten.middle.module.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class AttendanceMqConsumer {

    @Autowired
    @Qualifier("targetJdbcTemplate")
    private JdbcTemplate targetJdbcTemplate;

    @Autowired
    private SnowflakeIdWorker snowflakeIdWorker;

    /**
     * 监听队列，执行远程同步逻辑
     */
    @RabbitListener(queues = AttendanceMqConstants.QUEUE_ATTEN_SYNC)
    public void receiveMessage(Map<String, Object> data) {
        log.info("MQ 接收到同步任务: {}", data);

        // 1. 获取并格式化时间 (保持你原有的逻辑)
        String formattedTime = formatDkTime(data.get("dktime"));
        if (formattedTime == null || formattedTime.isEmpty()) {
            log.warn("时间字段为空，跳过同步");
            return;
        }

        String pin = String.valueOf(data.get("empid"));
        String deviceId = String.valueOf(data.get("clocksno"));

        // --- 关键修复：幂等性检查 ---
        // 根据 员工工号 + 设备号 + 打卡时间 判定唯一性
        String checkSql = "SELECT COUNT(*) FROM uf_ysdkjl WHERE empid = ? AND clocksno = ? AND dktime = ?";

        try {
            Integer count = targetJdbcTemplate.queryForObject(checkSql, Integer.class, pin, deviceId, formattedTime);

            if (count != null && count > 0) {
                log.info("数据已存在，跳过重复插入: PIN={}, Time={}", pin, formattedTime);
                return; // 结束方法，不再往下走 INSERT
            }
        } catch (Exception e) {
            log.error("幂等性检查查询失败: {}", e.getMessage());
            throw new RuntimeException("查询远程库失败，触发重试", e);
        }

        // --- 执行插入 ---
        long id = snowflakeIdWorker.nextId();
        String createTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
        String tenantKey = "ttcubrch31";

        String insertSql = "INSERT INTO uf_ysdkjl (id, FORM_DATA_ID, DATA_INDEX, CREATE_TIME, TENANT_KEY, clocksno, empid, dktime, is_flow, data_status) " +
                "VALUES (?, ?, 0, ?, ?, ?, ?, ?, 0, 1)";

        try {
            targetJdbcTemplate.update(insertSql, id, id, createTime, tenantKey, deviceId, pin, formattedTime);
            log.info("远程库同步成功: PIN={}", pin);
        } catch (Exception e) {
            log.error("远程插入失败: {}", e.getMessage());
            throw new RuntimeException("写入远程库失败，触发重试", e);
        }
    }

    /**
     * 提取时间格式化逻辑，让主逻辑更清晰
     */
    private String formatDkTime(Object timeObj) {
        if (timeObj == null) return "";
        try {
            String timeStr = String.valueOf(timeObj);
            java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(
                    timeStr, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
            );
            return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            return String.valueOf(timeObj).replace("T", " ");
        }
    }
}