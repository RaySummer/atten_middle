package com.ray.atten.middle.module.repository;

import com.ray.atten.middle.module.model.CardTemplate;
import com.ray.atten.middle.module.model.OaEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardTemplateRepository extends JpaRepository<CardTemplate, Long>, JpaSpecificationExecutor<CardTemplate> {

    List<CardTemplate> findByActiveTrueOrderByIdAsc();

    CardTemplate findByNameContains(String name);

    Optional<CardTemplate> findByUuid(UUID uuid);

    // 4. 注意：这里原代码返回的是 OaEmployee，如果是在 CardTemplate 里查询，应该返回模板集合
    // 如果是查询包含某些 UUID 的模板：
    List<CardTemplate> findByUuidIn(List<UUID> uuids);

    /**
     * 新增：根据公司名称集合查询模板
     * 使用 Distinct 关键字防止因为一个模板关联多个公司而查出重复记录
     */
    List<CardTemplate> findDistinctByCompanyNamesIn(Collection<String> companyNames);

    /**
     * 新增：查询活跃且属于某些公司的模板
     */
    List<CardTemplate> findDistinctByActiveTrueAndCompanyNamesIn(Collection<String> companyNames);
}