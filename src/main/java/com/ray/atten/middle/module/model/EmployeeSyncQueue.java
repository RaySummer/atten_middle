package com.ray.atten.middle.module.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Access(AccessType.FIELD)
@Entity
@Table(name = "employee_sync_queue")
public class EmployeeSyncQueue extends BaseEntity implements Serializable {

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

    //指纹或照片 base64后的数据存储字段
    @Column(columnDefinition = "TEXT")
    @Type(type = "text")
    private String base64Data;

    // 0: 待同步, 1: 已处理
    private Integer status;

    //：描述模版0无效模版 1正常模版
    private Integer valid = 1;

    //手指编号，取值为0到9
    private Integer fid = 0;

    //base64编码长度
    private Integer base64Size;

    //指纹   照片
    private String type;

    private String cardNo;

    //失败重试次数
    private Integer retry;

    //是否覆盖 0不覆盖返回错误，1覆盖
    private Integer overwrite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "pin",
            referencedColumnName = "pin",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT) // 关键：禁止生成外键约束
    )
    private OaEmployee oaEmployee;

    @Override
    public void onCreate() {
        super.onCreate();
        if (this.status == null) this.status = 0; // 默认待同步
        if (this.pri == null) this.pri = 0;
        if (this.verify == null) this.verify = 0;
        if (this.valid == null) this.valid = 1;
        if (this.base64Size == null) this.base64Size = 0;
        if (this.retry == null) this.retry = 0;
        if (this.overwrite == null) this.overwrite = 1;
    }

}
