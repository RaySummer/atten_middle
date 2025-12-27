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
public class CardTemplateDto extends BaseDto implements Serializable {
    private String name;
    private String description;
    private String layout;
    private Boolean active;

    public static CardTemplateDto convertToDto(CardTemplate template) {
        if (template == null) {
            return null;
        }
        CardTemplateDto dto = new CardTemplateDto();
        dto.setUuid(template.getUuid());
        dto.setName(template.getName());
        dto.setDescription(template.getDescription());
        dto.setLayout(template.getLayout());
        dto.setActive(template.getActive());

        return dto;
    }

    public static List<CardTemplateDto> listConvertToDto(List<CardTemplate> templates) {
        if (templates.isEmpty()) {
            return null;
        }
        return templates.stream().map(CardTemplateDto::convertToDto).collect(Collectors.toList());
    }

}
