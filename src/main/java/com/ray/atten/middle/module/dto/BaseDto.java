package com.ray.atten.middle.module.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseDto implements Serializable {

    protected UUID uuid;

    protected LocalDateTime createTime;

    protected LocalDateTime updateTime;
}
