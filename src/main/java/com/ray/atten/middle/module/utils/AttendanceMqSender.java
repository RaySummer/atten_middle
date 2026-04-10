package com.ray.atten.middle.module.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class AttendanceMqSender {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送打卡记录到 MQ
     *
     * @param recordMap 包含 pin, time, deviceId 等信息的 Map 或对象
     */
    public void sendSyncMessage(Map<String, Object> recordMap) {
        try {
            log.info("准备发送打卡记录到队列: empid={}", recordMap.get("empid"));

            // 发送到指定的交换机和路由键
            rabbitTemplate.convertAndSend(
                    AttendanceMqConstants.EXCHANGE_ATTEN_SYNC,
                    AttendanceMqConstants.ROUTING_ATTEN_SYNC,
                    recordMap
            );

        } catch (Exception e) {
            log.error("MQ 消息发送失败，数据：{}，错误：{}", recordMap, e.getMessage());
            // 这里可以考虑将失败记录写入本地文件或数据库作为备份
        }
    }
}