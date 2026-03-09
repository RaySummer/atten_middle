package com.ray.atten.middle.module.controller;

import com.ray.atten.middle.module.aspect.LogOperation;
import com.ray.atten.middle.module.model.AdminUser;
import com.ray.atten.middle.module.model.Company;
import com.ray.atten.middle.module.repository.AdminUserRepository;
import com.ray.atten.middle.module.utils.JwtUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthController(AdminUserRepository adminUserRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtils jwtUtils) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @LogOperation("登录")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        // 1. 查找用戶
        AdminUser user = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用戶不存在"));

        // 2. 驗證密碼
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(401).body("密碼錯誤");
        }

        // 3. 獲取該管理員管轄的公司名稱列表
        // 如果是超級管理員，我們可以傳一個特殊的標識，或者在後續邏輯中判斷
        List<String> companyNames = user.getManagedCompanies().stream()
                .map(Company::getName)
                .collect(Collectors.toList());

        // 4. 生成 Token
        String token = jwtUtils.generateToken(user.getUsername(), user.getIsSuperAdmin(), companyNames);

        // 5. 返回結果
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("username", user.getUsername());
        response.put("isSuperAdmin", user.getIsSuperAdmin());
        response.put("companies", companyNames);

        return ResponseEntity.ok(response);
    }

    /**
     * 验证 Token 有效性并返回当前用户信息
     * 只有携带有效 Token 的请求才能通过 Spring Security 过滤器到达这里
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken() {
        // 1. 从安全上下文中获取当前登录用户信息
        // 这些信息是由 JwtAuthenticationFilter 在解析 Token 后存入的
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token 已失效或未登录");
        }

        // 2. 构造返回给 Desktop 端的信息
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", auth.getName());

        // 3. 提取角色并判断是否为超管
        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        userInfo.put("roles", roles);
        userInfo.put("isSuperAdmin", roles.contains("ROLE_SUPER_ADMIN"));

        return ResponseEntity.ok(userInfo);
    }
}