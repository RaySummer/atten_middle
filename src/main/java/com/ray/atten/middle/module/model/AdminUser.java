package com.ray.atten.middle.module.model;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

@Entity
@Table(name = "sys_admin_user")
@Data
public class AdminUser extends BaseEntity implements UserDetails, Serializable {

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    // 多对多映射：一个管理员可以管理多个公司
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "sys_admin_company",
            joinColumns = @JoinColumn(name = "admin_id"),
            inverseJoinColumns = @JoinColumn(name = "company_id")
    )
    private Set<Company> managedCompanies; // 该管理员管辖的所有公司

    // 是否為超級管理員（可選，用於邏輯判斷）
    private Boolean isSuperAdmin = false;


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 暫時返回空集合，如果以後有角色（如 ADMIN/USER）可在這裡添加
        return Collections.emptyList();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}