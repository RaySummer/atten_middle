package com.ray.atten.middle.module.aspect;

import com.ray.atten.middle.module.model.AdminUser;
import com.ray.atten.middle.module.repository.AdminUserRepository;
import com.ray.atten.middle.module.utils.SecurityUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Collections;
import java.util.List;

@Aspect
@Component
public class DataPermissionAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private AdminUserRepository adminUserRepository;

    // 1. 缩小拦截范围：只拦截具体的业务 Service，避开安全认证相关的类
    @Before("execution(* com.ray.atten.middle.module.service..*.*(..)) " +
            "&& !execution(* com.ray.atten.middle.module.utils.DataInitializer.*(..))")
    public void beforeService() {
        // 获取当前认证信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 2. 严谨的判空：如果是匿名用户或未认证，直接退出，不执行过滤
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return;
        }

        String username = authentication.getName();

        // 3. 这里的查询要预防查不到的情况
        AdminUser admin = adminUserRepository.findByUsername(username).orElse(null);
        if (admin == null || admin.getIsSuperAdmin()) {
            return;
        }

        // 4. 获取公司列表
        List<String> myCompanies = SecurityUtils.getCurrentManagedCompanies();
        if (myCompanies == null || myCompanies.isEmpty()) {
            // 如果该账号没绑定公司，为了安全，注入一个不存在的公司名，让他查不到任何数据
            enableFilter(Collections.singletonList("EMPTY_PERMISSION_LOCK"));
            return;
        }

        enableFilter(myCompanies);
    }

    private void enableFilter(List<String> companies) {
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("companyFilter")
                .setParameterList("names", companies);
    }
}
