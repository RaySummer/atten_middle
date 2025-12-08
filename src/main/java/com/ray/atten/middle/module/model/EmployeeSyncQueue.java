package com.ray.atten.middle.module.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Access(AccessType.FIELD)
@Entity
@Table(name = "employee_sync_queue")
public class EmployeeSyncQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 指定推送到哪台机器 (如果为空，则推送到所有机器)
    // --- 同步控制 ---
    private String targetDeviceSn;

    // 工号
    private String pin;
    // 姓名
    private String name;

    private String passwd;
    //用户权限值0普通 2登记 6管理员 14超级管理员
    private Integer pri;
    //验证方式0自动识别 1指纹 15人脸
    private Integer verify;

    // 指纹模板 (Base64字符串)
    @Column(columnDefinition = "TEXT")
    private String fingerprint;

    // --- 修改点：照片改为 Base64 字符串存储 ---
    @Lob
    @Column(columnDefinition = "TEXT")
    private String photoBase64;

    // 0: 待同步, 1: 已处理
    private Integer status;

    //：描述模版0无效模版 1正常模版
    private Integer valid;

    //手指编号，取值为0到9
    private Integer fid;

    //指纹模版二进制数据经过base64编码之后的长度
    private Integer fingerSize;

    private Integer photoSize;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    //生物识别类型0通用的 1指纹 2面部 9可见光面部
    private String type;

    private String cardNo;

    //失败重试次数
    private Integer retry;

    //是否覆盖 0不覆盖返回错误，1覆盖
    private Integer overwrite;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        if (this.status == null) this.status = 0; // 默认待同步
        if (this.pri == null) this.pri = 0;
        if (this.verify == null) this.verify = 0;
        if (this.valid == null) this.valid = 1;
        if (this.fingerSize == null) this.fingerSize = 0;
        if (this.photoSize == null) this.photoSize = 0;
        if (this.retry == null) this.retry = 0;
        if (this.overwrite == null) this.overwrite = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }

}
