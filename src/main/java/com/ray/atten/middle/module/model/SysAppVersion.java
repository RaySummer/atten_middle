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
@Table(name = "sys_app_version")
public class SysAppVersion extends BaseEntity implements Serializable {

    private String versionCode;

    private String downloadUrl;

    private String updateLog;

    private Boolean forceUpdate;

}
