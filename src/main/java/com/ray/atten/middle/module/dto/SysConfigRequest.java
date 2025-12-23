package com.ray.atten.middle.module.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SysConfigRequest implements Serializable {

    @NonNull
    private String key;
    @NonNull
    private String value;

    private String desc;

}
