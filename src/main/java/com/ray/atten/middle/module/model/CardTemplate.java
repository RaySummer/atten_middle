package com.ray.atten.middle.module.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Access(AccessType.FIELD)
@Entity
@Table(name = "card_template")
public class CardTemplate extends BaseEntity implements Serializable {

    @Column(unique = true, nullable = false)
    private String name;

    private String description; // 模板描述

    private String layout; // 布局类型: vertical (竖版), horizontal (横版)

    private Integer baseWidth; // 基准宽度 (px)，用于前端计算 Grid 排版

    private String bgImageUrl; // 背景图 URL 或服务器相对路径

    @Column(columnDefinition = "TEXT")
    private String htmlContent;
    @Column(columnDefinition = "TEXT")
    private String cssContent;

    // 是否激活
    private Boolean active = Boolean.TRUE;

    @Override
    protected void onCreate() {
        super.onCreate(); // 1. 先执行父类的 UUID 生成逻辑
        if (baseWidth == null || baseWidth > 1) {
            if (layout.equalsIgnoreCase("vertical")) {
                this.baseWidth = 370;
            }
            if (layout.equalsIgnoreCase("horizontal")) {
                this.baseWidth = 230;
            }
        }
    }

}
