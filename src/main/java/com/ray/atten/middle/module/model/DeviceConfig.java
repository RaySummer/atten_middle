package com.ray.atten.middle.module.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Access(AccessType.FIELD)
public class DeviceConfig implements Serializable {

    private String sn;

    // 考勤记录时间戳 (默认 0，表示全同步)
    private long attLogStamp = 0;

    // 操作日志时间戳 (默认 0)
    private long operLogStamp = 0;

    // 生物模板时间戳 (默认 0)
    private long bioDataStamp = 0;

    // 其他默认配置项
    private int delay = 10;
    private int realtime = 1;

    // 构造函数用于初始化 SN
    public DeviceConfig(String sn) {
        this.sn = sn;
    }

    /**
     * 【时间戳优化方法】
     * 传入最新的 LocalDateTime，如果它比当前记录的时间戳新，则更新。
     * 保证了 Stamp 永远是该类型数据已同步的最大时间戳。
     * * @param latestTime 最新考勤记录时间
     */
    public synchronized void updateAttLogStamp(LocalDateTime latestTime) {
        // 使用东八区时间转换为 Unix 秒级时间戳
        long newStamp = latestTime.toEpochSecond(ZoneOffset.ofHours(8));

        // 核心逻辑：只有新时间戳大于当前时间戳时才更新
        if (newStamp > this.attLogStamp) {
            this.attLogStamp = newStamp;
        }
    }

}
