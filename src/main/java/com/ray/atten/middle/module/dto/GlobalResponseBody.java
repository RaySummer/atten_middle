package com.ray.atten.middle.module.dto;

import lombok.*;

import java.io.Serializable;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GlobalResponseBody implements Serializable {

    private String status;

    private String msg;

    private String content;

}
