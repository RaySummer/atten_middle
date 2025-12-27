package com.ray.atten.middle.module.service;

import com.ray.atten.middle.module.dto.*;
import com.ray.atten.middle.module.model.CardTemplate;
import com.ray.atten.middle.module.repository.CardTemplateRepository;
import com.ray.atten.middle.module.repository.OaEmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardTemplateService {

    private final CardTemplateRepository repository;
    private final OaEmployeeRepository oaEmployeeRepository;

    /**
     * 获取所有可用模板列表
     */
    public List<CardTemplateDto> findAllActive() {
        return CardTemplateDto.listConvertToDto(repository.findByActiveTrue());
    }

    /**
     * 保存或更新模板
     */
    @Transactional
    public CardTemplateResponseDto saveTemplate(CardTemplateRequest request) {
        CardTemplate template;
        // 改为根据 UUID 判断更新还是新增
        if (request.getUuid() != null) {
            template = repository.findByUuid(request.getUuid())
                    .orElseThrow(() -> new RuntimeException("模板不存在"));
        } else {
            template = new CardTemplate();
        }

        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setLayout(request.getLayout());
        template.setHtmlContent(request.getHtmlContent());
        template.setCssContent(request.getCssContent());
        template.setBgImageUrl(request.getBgImageUrl());
        template.setActive(request.getActive() != null ? request.getActive() : true);

        // 业务逻辑：如果 baseWidth 为空，根据布局自动设定
        if (request.getBaseWidth() == null || request.getBaseWidth() <= 0) {
            template.setBaseWidth("horizontal".equalsIgnoreCase(request.getLayout()) ? 370 : 230);
        } else {
            template.setBaseWidth(request.getBaseWidth());
        }

        return CardTemplateResponseDto.convertToDto(repository.save(template));
    }

    /**
     * 根据 UUID 获取单个模板详情
     */
    public CardTemplateResponseDto getByUuid(UUID uuid) {
        return CardTemplateResponseDto.convertToDto(repository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("模板不存在")));
    }

    /**
     * 根据 UUID 删除模板
     */
    @Transactional
    public void deleteTemplateByUuid(UUID uuid) {
        // JPA 通常建议先查再删，或者在 Repository 定义 deleteByUuid
        CardTemplate template = repository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("模板不存在"));
        repository.delete(template);
    }

    /**
     * 切换模板激活状态
     */
    @Transactional
    public void toggleActiveByUuid(UUID uuid) {
        CardTemplate template = repository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("模板不存在"));
        template.setActive(!template.getActive());
        repository.save(template);
    }

    /**
     * 根据模板名称获取模板
     */
    public CardTemplateResponseDto getByName(String name) {
        return CardTemplateResponseDto.convertToDto(repository.findByNameContains(name));
    }

    /**
     * 获取打印载荷：封装模板信息和选中的员工列表 (全部使用 UUID)
     *
     * @param templateUuid  选中的模板 UUID
     * @param employeeUuids 选中的员工 UUID 集合
     * @return 打印页面所需的完整数据
     */
    public PrintPayloadDto getPrintPayload(UUID templateUuid, List<UUID> employeeUuids) {
        // 1. 获取并转换模板 DTO
        CardTemplate template = repository.findByUuid(templateUuid)
                .orElseThrow(() -> new RuntimeException("选中的模板不存在 (UUID: " + templateUuid + ")"));

        CardTemplateResponseDto templateDto = CardTemplateResponseDto.convertToDto(template);

        // 2. 查询并按照传入的 UUID 顺序排序
        // 注意：Repository 需要支持 findAllByUuidIn(List<UUID> uuids)
        Map<UUID, OaEmployeeDto> employeeMap = oaEmployeeRepository.findAllByUuidIn(employeeUuids)
                .stream()
                .map(OaEmployeeDto::convertToDto)
                .collect(Collectors.toMap(OaEmployeeDto::getUuid, emp -> emp));

        // 按照传入的 employeeUuids 顺序提取，保证打印顺序
        List<OaEmployeeDto> employeeDtos = employeeUuids.stream()
                .map(employeeMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (employeeDtos.isEmpty()) {
            log.warn("打印请求中未找到有效的员工数据，UUID 列表: {}", employeeUuids);
        }

        return new PrintPayloadDto(templateDto, employeeDtos);
    }
}