package com.ray.atten.middle.module.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/debug")
public class DbTestController {

    @Autowired
    @Qualifier("targetDataSource")
    private DataSource targetDataSource;

    @Autowired
    @Qualifier("targetJdbcTemplate")
    private JdbcTemplate targetJdbcTemplate;

    @GetMapping("/test-remote-db")
    public Map<String, Object> testConnection() {
        Map<String, Object> result = new HashMap<>();

        try (Connection conn = targetDataSource.getConnection()) {
            // 1. 测试基础连接
            result.put("status", "success");
            result.put("message", "数据库连接成功！");
            result.put("catalog", conn.getCatalog());

            // 2. 测试 SQL 执行能力
            Integer val = targetJdbcTemplate.queryForObject("SELECT 1", Integer.class);
            result.put("sql_test", "SELECT 1 返回结果: " + val);

            // 3. 检查目标表是否存在（请替换为你真实的表名）
            // String tableName = "your_actual_table_name";
            // Integer count = targetJdbcTemplate.queryForObject(
            //    "SELECT COUNT(*) FROM " + tableName, Integer.class);
            // result.put("table_check", tableName + " 当前记录数: " + count);

        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "连接失败: " + e.getMessage());
            result.put("error_type", e.getClass().getName());
        }

        return result;
    }
}