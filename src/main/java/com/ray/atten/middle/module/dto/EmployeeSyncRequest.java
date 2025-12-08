package com.ray.atten.middle.module.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class EmployeeSyncRequest implements Serializable {

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
    private String fingerprint;

    // --- 修改点：照片改为 Base64 字符串存储 ---
    private String photoBase64;

    // 指定推送到哪台机器 (如果为空，则推送到所有机器)
    //多台考勤机用英文符号的逗号隔开
    // --- 同步控制 ---
    private String deviceSn;

    //描述模版0无效模版 1正常模版
    private Integer valid;

    //手指编号，取值为0到9
    private Integer fid;

    //指纹模版二进制数据经过base64编码之后的长度
    private Integer fingerSize;

    private Integer photoSize;

    private String cardNo;

    //失败重试次数
    private Integer retry;

    //是否覆盖 0不覆盖返回错误，1覆盖
    private Integer overwrite;

}
