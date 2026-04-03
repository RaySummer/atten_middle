package com.ray.atten.middle.module.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
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
        log.info("MQ 接收到同步任务，开始写入远程库: {}", data);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String formattedTime = "";
        Object timeObj = data.get("dktime");
        try {
            if (timeObj != null) {
                String timeStr = String.valueOf(timeObj);

                // 1. 处理 ISO 格式 (例如 2026-04-01T11:25)
                // 使用 Java 8 的 LocalDateTime 自动解析带 T 的字符串
                java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(
                        timeStr,
                        java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
                );

                // 2. 转换为目标格式的字符串 (yyyy-MM-dd HH:mm:ss)
                formattedTime = dateTime.format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                );
            }
        } catch (Exception e) {
            log.error("时间解析失败 [{}]: {}", timeObj, e.getMessage());
            // 备选方案：如果解析失败，尝试最后一次强行替换 T 为空格
            formattedTime = String.valueOf(timeObj).replace("T", " ");
        }


        long id = snowflakeIdWorker.nextId();
        long formDataId = id;
        String createTime = simpleDateFormat.format(new java.util.Date());
        String tenantKey = "ttcubrch31";
        String pin = String.valueOf(data.get("empid"));
        String deviceId = String.valueOf(data.get("clocksno"));

        // 执行远程 SQL 插入
        String sql = "INSERT INTO uf_ysdkjl (id, FORM_DATA_ID, DATA_INDEX, CREATE_TIME, TENANT_KEY, clocksno, empid, dktime, is_flow, data_status) " +
                "VALUES (?, ?, 0, ?, ?, ?, ?, ?, 0, 1)";

        try {
            targetJdbcTemplate.update(sql, id, formDataId, createTime, tenantKey, deviceId, pin, formattedTime);
            log.info("远程库同步成功: PIN={}", pin);
        } catch (Exception e) {
            log.error("远程同步 SQL 执行失败: {}", e.getMessage());
            // 如果报错，由于没有手动 Ack，消息会根据配置重试或进入死信队列
            throw new RuntimeException("同步失败，触发 MQ 重试", e);
        }
    }
}