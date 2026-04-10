package com.ray.atten.middle.module.model;

import com.ray.atten.middle.module.utils.SecurityConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Access(AccessType.FIELD)
@Entity
@Table(name = "card_template")
// 1. 定义过滤器参数
@FilterDef(
        name = SecurityConstants.COMPANY_FILTER_NAME,
        parameters = @ParamDef(name = SecurityConstants.COMPANY_PARAM_NAME, type = "string")
)
@Filter(
        name = SecurityConstants.COMPANY_FILTER_NAME,
        condition = "id IN (SELECT tc.template_id FROM card_template_companies tc WHERE tc.company_name IN (:" + SecurityConstants.COMPANY_PARAM_NAME + "))"
)
public class CardTemplate extends BaseEntity implements Serializable {

    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    private String layout;

    private Integer baseWidth;

    private String bgImageUrl;

    @Column(columnDefinition = "TEXT")
    private String htmlContent;

    @Column(columnDefinition = "TEXT")
    private String cssContent;

    @Column(columnDefinition = "TEXT")
    private String bgImageBase;

    private Boolean active = Boolean.TRUE;

    // --- 核心关联配置 ---
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "card_template_companies",
            joinColumns = @JoinColumn(name = "template_id") // 对应子查询中的 template_id
    )
    @Column(name = "company_name") // 对应子查询中的 company_name
    private Set<String> companyNames = new HashSet<>();

    @Override
    protected void onCreate() {
        super.onCreate();
        if (baseWidth == null || baseWidth <= 1) { // 修正判断逻辑
            if ("vertical".equalsIgnoreCase(layout)) {
                this.baseWidth = 370;
            } else if ("horizontal".equalsIgnoreCase(layout)) {
                this.baseWidth = 230;
            }
        }
    }
}