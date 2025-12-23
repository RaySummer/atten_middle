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
public class SysAppVersion implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String versionCode;

    private String downloadUrl;

    private String updateLog;

    private Boolean forceUpdate;

    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }

}
