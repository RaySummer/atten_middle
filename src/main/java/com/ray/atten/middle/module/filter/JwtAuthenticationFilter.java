package com.ray.atten.middle.module.filter;

import com.ray.atten.middle.module.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;

    public JwtAuthenticationFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        try {
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);

                if (jwtUtils.validateToken(token)) {
                    String username = jwtUtils.getUsernameFromToken(token);
                    // 1. 從 Token 中提取角色字串 (ROLE_ADMIN 或 ROLE_SUPER_ADMIN)
                    String role = jwtUtils.getRoleFromToken(token);
                    List<String> companies = jwtUtils.getCompaniesFromToken(token);

                    // 2. 將角色字串封裝為 Spring Security 識別的 GrantedAuthority
                    // 注意：這裡必須傳入權限列表，否則 hasRole 判斷會失效
                    List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(role));

                    // 3. 構造 Authentication 物件
                    // 第三個參數傳入 authorities 後，authenticated 狀態會自動變為 true
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            username, null, authorities);

                    // 4. 將公司清單存入 Details，供後續 DataPermissionAspect 使用
                    auth.setDetails(companies);

                    // 5. 存入上下文
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    log.debug("User {} authenticated with role {}", username, role);
                }
            }
        } catch (Exception e) {
            log.error("Security Context setting failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}
