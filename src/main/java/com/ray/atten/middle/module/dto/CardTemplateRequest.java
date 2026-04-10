package com.ray.atten.middle.module.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CardTemplateRequest extends BaseDto implements Serializable {
    @NotBlank(message = "模板名称不能为空")
    private String name;

    private String description;

    @NotBlank(message = "布局类型不能为空")
    private String layout;

    private Integer baseWidth;

    private String bgImageUrl;

    private String bgImageBase;

    @NotBlank(message = "HTML内容不能为空")
    private String htmlContent;

    private String cssContent;

    private Boolean active = Boolean.TRUE;

    /**
     * 新增：支持关联多个公司
     * 使用 List 接收前端的勾选列表
     */
    private List<String> companyNames;
}