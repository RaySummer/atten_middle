package com.ray.atten.middle.module.controller;

import com.ray.atten.middle.module.dto.GlobalResponseBody;
import com.ray.atten.middle.module.dto.SysConfigRequest;
import com.ray.atten.middle.module.service.SysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/config")
public class SysConfigController {

    @Autowired
    private SysConfigService configService;

    /**
     * 修改或新增配置项
     * 例如：POST /api/admin/config/update?key=UPDATE_FILES_DIR&value=D:/my_updates/
     */
    @PostMapping("/update")
    public ResponseEntity<GlobalResponseBody> updateConfig(@RequestBody SysConfigRequest request) {
        try {
            configService.saveOrUpdateConfig(request);
            return ResponseEntity.ok(new GlobalResponseBody("200", "SUCCESS", "配置更新成功并已同步内存"));
        } catch (Exception e) {
            return ResponseEntity.ok(new GlobalResponseBody("500", "ERROR", "更新失败: " + e.getMessage()));
        }
    }

    /**
     * 手动触发刷新缓存（备用接口）
     */
    @GetMapping("/refresh")
    public ResponseEntity<GlobalResponseBody> refresh() {
        configService.refreshCache();
        return ResponseEntity.ok(new GlobalResponseBody("200", "SUCCESS", "内存缓存刷新完成"));
    }
}
