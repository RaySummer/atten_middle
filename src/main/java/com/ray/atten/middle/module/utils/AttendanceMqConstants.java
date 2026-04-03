package com.ray.atten.middle.module.utils;

public class AttendanceMqConstants {

    // 交换机名称
    public static final String EXCHANGE_ATTEN_SYNC = "exc.atten.sync.direct";

    // 队列名称
    public static final String QUEUE_ATTEN_SYNC = "q.atten.sync.remote";

    // 路由键
    public static final String ROUTING_ATTEN_SYNC = "key.atten.sync";

}
