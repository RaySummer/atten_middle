package com.ray.atten.middle.module.config;

import com.ray.atten.middle.module.filter.JwtAuthenticationFilter;
import com.ray.atten.middle.module.utils.JwtUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtUtils jwtUtils;

    public SecurityConfig(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // 使用强哈希加密
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable() // 因为是 JWT，不需要 CSRF 防护
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 禁用 Session
                .and()
                .authorizeRequests()
                .antMatchers("/api/auth/login", "/error", "/ping").permitAll() // 允许匿名访问
                // 允许匿名访问 Swagger 数据接口
                .antMatchers("/swagger-ui.html", "/swagger-resources/**", "/v2/api-docs", "/v3/api-docs", "/webjars/**").permitAll()
                .antMatchers("/iclock/**", "/api/download/**", "/api/card-template/**", "/api/version/**", "/api/oa-employees/push", "/badge_print.html").permitAll() // 允许匿名访问
                .antMatchers("/api/admin/**").hasRole("SUPER_ADMIN") // 限制只有超管能访问管理接口
                .anyRequest().authenticated(); // 其他所有接口都需要登录

        // 把 JWT 过滤器加进流程中
        http.addFilterBefore(new JwtAuthenticationFilter(jwtUtils), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .antMatchers("/css/**", "/js/**", "/images/**", "/lib/**", "/favicon.ico")
                .antMatchers("/**/*.html", "/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg")
                // --- 新增：Swagger 核心路径 ---
                .antMatchers("/swagger-ui.html")     // Swagger 入口
                .antMatchers("/swagger-resources/**") // Swagger 资源配置
                .antMatchers("/v2/api-docs")          // 接口文档数据 (重要)
                .antMatchers("/v3/api-docs")          // OpenAPI 3 数据
                .antMatchers("/webjars/**");          // Swagger UI 依赖的静态资源;
    }
}