package com.ray.atten.middle.module.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class DeviceCommandRequest implements Serializable {

    /**
     * device sn
     */
    private List<String> deviceSns;

    /**
     * cmd -> DATA, ATTLOG, OPERLOG, ATTPHOTO, BIODATA, IDCARD, ERRORLOG, CHECK, LOG
     */
    private String cmd;

    /**
     * recode -> UPDATA, DELETE, QUERY, CLEAR
     */
    private String recode;

    /**
     * table -> ATTLOG, ATTPHOTO, FINGERTMP, BIODATA, USERINFO, BIOPHOTO
     */
    private String table;

    /**
     * IDCard
     */
    private String pin;

    /**
     * Attendance time
     */
    private LocalDateTime startTime;

    /**
     * Attendance time
     */
    private LocalDateTime endTime;

    /**
     * finger
     */
    private String FID;

}
