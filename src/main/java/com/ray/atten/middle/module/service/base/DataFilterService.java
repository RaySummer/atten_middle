package com.ray.atten.middle.module.service.base;

import com.ray.atten.middle.module.model.AdminUser;
import com.ray.atten.middle.module.repository.AdminUserRepository;
import com.ray.atten.middle.module.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
public class DataFilterService {

    @Autowired
    private AdminUserRepository adminUserRepository;

    /**
     * 通用数据过滤提取方法
     *
     * @param finder    传入一个函数，接收公司列表并返回查询结果
     * @param allFinder 传入一个函数，用于超级管理员获取全部数据
     */
    public <T> List<T> getFilterData(Function<List<String>, List<T>> finder, Supplier<List<T>> allFinder) {
        String username = SecurityUtils.getCurrentUsername();
        AdminUser admin = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("当前用户不存在"));

        if (admin.getIsSuperAdmin()) {
            return allFinder.get();
        } else {
            List<String> myCompanies = SecurityUtils.getCurrentManagedCompanies();
            return finder.apply(myCompanies);
        }
    }
}