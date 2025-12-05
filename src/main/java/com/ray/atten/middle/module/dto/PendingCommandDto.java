package com.ray.atten.middle.module.dto;

import com.ray.atten.middle.module.model.DeviceCommand;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class PendingCommandDto implements Serializable {

    private String commands;

    private DeviceCommand deviceCommands;

}
