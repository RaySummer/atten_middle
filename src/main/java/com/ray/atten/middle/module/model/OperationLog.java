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
@Table(name = "operation_log")
public class OperationLog implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 考勤机序列号 (唯一标识)
    @Column(unique = true, nullable = false)
    private String deviceSn;

    /**
     * 操作： 1考勤，2人员，3其他。。。
     */
    private Integer operation;

    private LocalDateTime operationTime;

    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        this.operationTime = LocalDateTime.now();
        this.createTime = LocalDateTime.now();
    }

}
