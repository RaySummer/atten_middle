package com.ray.atten.middle.module.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class JwtUtils {

    @Value("${spring.security.secret}")
    private String secret = "atten_middle_secret_key_2026"; // 秘钥

    @Value("${spring.security.expiration}")
    private long expiration = 86400L; // 有效期 24 小时 (注意：配置文件中如果是秒，需要 * 1000)

    /**
     * 生成 Token
     * * @param username 用户名
     * @param isSuperAdmin 是否为超级管理员
     * @param companies 管辖公司名称列表
     * @return JWT Token
     */
    public String generateToken(String username, boolean isSuperAdmin, List<String> companies) {
        Map<String, Object> claims = new HashMap<>();

        // 1. 存入公司清单
        claims.put("companies", companies);

        // 2. 存入角色：Spring Security 默认匹配以 ROLE_ 开头的字符串
        String role = isSuperAdmin ? "ROLE_SUPER_ADMIN" : "ROLE_ADMIN";
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    /**
     * 从 Token 中获取用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * 从 Token 中获取角色字符串
     */
    public String getRoleFromToken(String token) {
        return (String) getClaimsFromToken(token).get("role");
    }

    /**
     * 从 Token 中获取公司清单
     */
    @SuppressWarnings("unchecked")
    public List<String> getCompaniesFromToken(String token) {
        Object companies = getClaimsFromToken(token).get("companies");
        return companies != null ? (List<String>) companies : new ArrayList<>();
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 验证 Token 有效性
     */
    public boolean validateToken(String token) {
        try {
            // 解析成功且未过期即为有效
            Claims claims = getClaimsFromToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            // 包含解析失败、过期、签名错误等所有异常
            return false;
        }
    }
}
