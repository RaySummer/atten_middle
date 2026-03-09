package com.ray.atten.middle.module.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
public class BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 保留 Long ID 作为物理主键（外键关联性能更好）

    @Type(type = "org.hibernate.type.UUIDCharType")
    @Column(name = "uuid", length = 36, unique = true, nullable = false,
            updatable = false, insertable = false) // 设置 insertable = false 让数据库接管插入
    @org.hibernate.annotations.Generated(org.hibernate.annotations.GenerationTime.INSERT)
    private UUID uuid;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        this.uuid = UUID.randomUUID();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }

}
