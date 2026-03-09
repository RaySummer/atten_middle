package com.ray.atten.middle.module.controller;

import com.ray.atten.middle.module.aspect.LogOperation;
import com.ray.atten.middle.module.dto.AdminCreateRequest;
import com.ray.atten.middle.module.dto.GlobalResponseBody;
import com.ray.atten.middle.module.model.Company;
import com.ray.atten.middle.module.service.AdminManagementService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminManagementService adminManagementService;

    /**
     * 获取所有公司列表
     */
    @GetMapping("/companies")
    public ResponseEntity<GlobalResponseBody> getAllCompanies() {
        return ResponseEntity.ok(new GlobalResponseBody("200", "SUCCESS", adminManagementService.getAllCompanies()));
    }

    /**
     * 删除公司
     */
    @LogOperation("删除公司")
    @DeleteMapping("/companies/{uuid}")
    public ResponseEntity<GlobalResponseBody> deleteCompany(@PathVariable UUID uuid) {
        adminManagementService.deleteCompany(uuid);
        return ResponseEntity.ok(new GlobalResponseBody("200", "SUCCESS", "删除成功"));
    }

    /**
     * 公司详情
     */
    @GetMapping("/companies/{uuid}")
    public ResponseEntity<GlobalResponseBody> getCompany(@PathVariable UUID uuid) {
        return ResponseEntity.ok(new GlobalResponseBody("200", "SUCCESS", adminManagementService.getCompanyByUuid(uuid)));
    }

    /**
     * 创建新公司
     */
    @LogOperation("创建新公司")
    @PostMapping("/companies")
    public ResponseEntity<GlobalResponseBody> createCompany(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (StringUtils.isEmpty(name)) {
            return ResponseEntity.ok(new GlobalResponseBody("500", "ERROR", "公司名字不能为空"));
        }
        return ResponseEntity.ok(new GlobalResponseBody("200", "SUCCESS", adminManagementService.createCompany(name)));
    }

    /**
     * 获取所有管理员
     */
    @GetMapping("/users")
    public ResponseEntity<GlobalResponseBody> getAllAdmins() {
        return ResponseEntity.ok(new GlobalResponseBody("200", "SUCCESS", adminManagementService.getAllAdmin()));
    }

    /**
     * 修改管理员密码，公司权限
     */
    @LogOperation("修改管理员密码，公司权限")
    @PutMapping("/users/{uuid}/permissions")
    public ResponseEntity<?> updatePermissions(@PathVariable UUID uuid, @RequestBody AdminCreateRequest request) {
        adminManagementService.updateAdminPermissions(uuid, request);
        return ResponseEntity.ok(new GlobalResponseBody("200", "SUCCESS", "权限修改成功"));
    }

    /**
     * 创建管理员并分配公司
     */
    @LogOperation("创建管理员并分配公司")
    @PostMapping("/users")
    public ResponseEntity<?> addAdmin(@RequestBody AdminCreateRequest request) {
        adminManagementService.createAdmin(request);
        return ResponseEntity.ok(new GlobalResponseBody("200", "SUCCESS", "创建成功"));
    }

    /**
     * 删除管理员账号
     */
    @LogOperation("删除管理员账号")
    @DeleteMapping("/users/{adminUuid}")
    public ResponseEntity<GlobalResponseBody> deleteAdmin(@PathVariable UUID adminUuid) {
        adminManagementService.deleteAdmin(adminUuid);
        return ResponseEntity.ok(new GlobalResponseBody("200", "SUCCESS", "删除成功"));
    }


}
