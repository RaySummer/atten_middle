package com.ray.atten.middle.module.dto;

import com.ray.atten.middle.module.model.CardTemplate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CardTemplateResponseDto extends BaseDto implements Serializable {
    private String name;
    private String description;
    private String layout;
    private Integer baseWidth;
    private String bgImageUrl;
    private String bgImageBase;
    private String htmlContent;
    private String cssContent;
    private Boolean active;

    public static CardTemplateResponseDto convertToDto(CardTemplate template) {
        if (template == null) {
            return null;
        }
        CardTemplateResponseDto dto = new CardTemplateResponseDto();
        dto.setUuid(template.getUuid());
        dto.setName(template.getName());
        dto.setDescription(template.getDescription());
        dto.setLayout(template.getLayout());
        dto.setBaseWidth(template.getBaseWidth());
        dto.setBgImageUrl(template.getBgImageUrl());
        dto.setBgImageBase(template.getBgImageBase());
        dto.setHtmlContent(template.getHtmlContent());
        dto.setCssContent(template.getCssContent());
        dto.setActive(template.getActive());

        return dto;
    }

    public static List<CardTemplateResponseDto> listConvertToDto(List<CardTemplate> templates) {
        if (templates.isEmpty()) {
            return null;
        }
        return templates.stream().map(CardTemplateResponseDto::convertToDto).collect(Collectors.toList());
    }

}
