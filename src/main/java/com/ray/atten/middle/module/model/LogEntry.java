package com.ray.atten.middle.module.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "sys_operation_log")
@Data
public class LogEntry extends BaseEntity {

    private String username;     // 用户名（或匿名用户）
    private String ip;           // 访问者IP
    private String method;       // 请求方式 (GET, POST等)
    private String apiPath;      // 接口路径
    private String className;    // 调用类名
    private String methodName;   // 调用方法名
    private String params;       // 请求参数 (建议截取长度防止过大)
    private Long executionTime;  // 执行耗时(毫秒)
}