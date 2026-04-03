package com.ray.atten.middle.module.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    // --- 显式声明主数据源，解决 jdbcUrl 丢失问题 ---
    @Primary
    @Bean(name = "dataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource() {
        // 使用 HikariDataSource 保证类型明确
        return DataSourceBuilder.create().build();
    }

    // --- 远程数据源 ---
    @Bean(name = "targetDataSource")
    @ConfigurationProperties(prefix = "remote.datasource")
    public DataSource targetDataSource() {
        return DataSourceBuilder.create().build();
    }

    // --- 远程 JdbcTemplate ---
    @Bean(name = "targetJdbcTemplate")
    public JdbcTemplate targetJdbcTemplate(@Qualifier("targetDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }
}