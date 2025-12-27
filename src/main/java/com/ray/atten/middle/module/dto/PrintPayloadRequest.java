package com.ray.atten.middle.module.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrintPayloadRequest implements Serializable {

    private UUID templateId;

    private List<UUID> employeeIds;
}
