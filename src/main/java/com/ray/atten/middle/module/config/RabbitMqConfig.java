package com.ray.atten.middle.module.config;

import com.ray.atten.middle.module.utils.AttendanceMqConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    // 1. 定义交换机 (Direct 模式)
    @Bean
    public DirectExchange attenSyncExchange() {
        return new DirectExchange(AttendanceMqConstants.EXCHANGE_ATTEN_SYNC, true, false);
    }

    // 2. 定义持久化队列
    @Bean
    public Queue attenSyncQueue() {
        return new Queue(AttendanceMqConstants.QUEUE_ATTEN_SYNC, true);
    }

    // 3. 绑定队列到交换机
    @Bean
    public Binding attenSyncBinding(Queue attenSyncQueue, DirectExchange attenSyncExchange) {
        return BindingBuilder.bind(attenSyncQueue)
                .to(attenSyncExchange)
                .with(AttendanceMqConstants.ROUTING_ATTEN_SYNC);
    }
}