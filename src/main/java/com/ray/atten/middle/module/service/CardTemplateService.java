package com.ray.atten.middle.module.service;

import com.ray.atten.middle.module.dto.*;
import com.ray.atten.middle.module.model.CardTemplate;
import com.ray.atten.middle.module.model.EmployeeSyncQueue;
import com.ray.atten.middle.module.model.OaEmployee;
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
        return CardTemplateDto.listConvertToDto(repository.findByActiveTrueOrderByIdAsc());
    }

    /**
     * 保存或更新模板
     */
    @Transactional
    public CardTemplateResponseDto saveTemplate(CardTemplateRequest request) {
        CardTemplate template;
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
        template.setBgImageBase(request.getBgImageBase());
        template.setActive(request.getActive() != null ? request.getActive() : true);

        // --- 核心修改：处理多公司关联 ---
        // 假设 request.getCompanyNames() 返回的是 List<String>
        if (request.getCompanyNames() != null) {
            // 注意：JPA 的 ElementCollection 建议先 clear 再 addAll，或者直接设置新集合
            // 如果使用直接设置，确保实体类中有相应的 Setter
            template.getCompanyNames().clear();
            template.getCompanyNames().addAll(request.getCompanyNames());
        }

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
        // 1. 获取模板
        CardTemplate template = repository.findByUuid(templateUuid)
                .orElseThrow(() -> new RuntimeException("选中的模板不存在 (UUID: " + templateUuid + ")"));
        CardTemplateResponseDto templateDto = CardTemplateResponseDto.convertToDto(template);

        // 2. 批量查询员工信息（注意：这里会自动关联查询 syncQueue）
        // 如果想要性能更好，建议在 Repository 使用 Fetch Join 或者 EntityGraph
        List<OaEmployee> employees = oaEmployeeRepository.findAllByUuidIn(employeeUuids);

        // 3. 将员工数据转为 Map，Key 为 UUID，方便后续排序
        Map<UUID, OaEmployeeDto> employeeMap = employees.stream()
                .map(emp -> {
                    // 转换基础信息 (PIN, Name, Dept 等)
                    OaEmployeeDto dto = OaEmployeeDto.convertToDto(emp);

                    // --- 核心修改：从 EmployeeSyncQueue 提取照片和指纹 ---
                    if (emp.getSyncQueue() != null) {
                        EmployeeSyncQueue sync = emp.getSyncQueue();

                        // 将 sync 表中的照片赋值给 DTO
                        // 确保你的 OaEmployeeDto 中有对应的字段 (例如 photoBase64)
                        dto.setPhotoBase64(sync.getPhotoBase64());
                    }
                    return dto;
                })
                .collect(Collectors.toMap(OaEmployeeDto::getUuid, emp -> emp));

        // 4. 按照传入的 UUID 顺序重排，保证打印顺序与勾选顺序一致
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