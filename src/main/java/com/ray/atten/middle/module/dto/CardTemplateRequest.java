package com.ray.atten.middle.module.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CardTemplateRequest extends BaseDto implements Serializable {
    @NotBlank
    private String name;
    private String description;
    @NotBlank
    private String layout;
    private Integer baseWidth;
    private String bgImageUrl;
    private String bgImageBase;
    @NotBlank
    private String htmlContent;
    private String cssContent;
    private Boolean active = Boolean.TRUE;
}
