package com.ray.atten.middle.module.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;

public class SecurityUtils {

    @SuppressWarnings("unchecked")
    public static List<String> getCurrentManagedCompanies() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return Collections.emptyList();
        }
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        if (details instanceof List) {
            return (List<String>) details;
        }
        return Collections.emptyList();
    }

    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null; // 返回 null 而不是报 NPE
        }
        return auth.getName();
    }
}
