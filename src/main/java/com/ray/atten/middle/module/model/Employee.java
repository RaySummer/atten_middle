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
@Table(name = "employee")
public class Employee extends BaseEntity implements Serializable {

    // 员工的工号/PIN，是主键
    @Column(unique = true, nullable = false)
    private String pin;

    // 员工姓名
    private String name;

    // 生物具体个体编号，默认值为0
    private String biologyNo;

    // 员工在设备上的权限等级 (例如 0:普通用户, 1:管理员)
    private Integer privilege;

    // 记录该员工是从哪个设备同步下来的
    private String syncedFromDeviceSn;

    //是否有效标示，0：无效，1：有效，默认为1
    private Integer valid;

    //生物识别类型
    //0通用的 1指纹 2面部 3声纹 4虹膜 5视网膜 6掌纹 7指静脉 8掌静脉 9可见光面部
    private Integer type;

    //可见光面部版本号
    private String majorVer;

    //副版本号
    private String minorVer;

    //模板格式 0ZK 1ISO 2ANSI
    private String format;

    //模板数据 对原始二进制指纹模版进行base64编码
    @Column(columnDefinition = "TEXT")
    private String tmp;

    // 同步到数据库的时间
    private LocalDateTime syncTime;

    @Override
    public void onCreate() {
        super.onCreate();
        this.syncTime = LocalDateTime.now();
        if (this.privilege == null) this.privilege = 0;
        if (this.biologyNo == null) this.biologyNo = "0";

    }
}
