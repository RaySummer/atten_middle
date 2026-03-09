package com.ray.atten.middle.module.controller;

import com.ray.atten.middle.module.aspect.LogOperation;
import com.ray.atten.middle.module.dto.GlobalResponseBody;
import com.ray.atten.middle.module.model.SysAppVersion;
import com.ray.atten.middle.module.service.SysAppVersionService;
import com.ray.atten.middle.module.utils.GlobalConfigHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/version")
public class SysAppVersionController {

    @Autowired
    private SysAppVersionService versionService;

    /**
     * 检查更新接口
     * 客户端调用：GET /api/version/check?currentVersion=1.0.0
     */
    @GetMapping("/check")
    public ResponseEntity<GlobalResponseBody> checkUpdate(@RequestParam String currentVersion) {
        SysAppVersion latest = versionService.getLatestVersion();

        if (latest == null) {
            return ResponseEntity.ok(new GlobalResponseBody("success", "已是最新版本", null));
        }

        boolean hasUpdate = versionService.isNewer(currentVersion, latest.getVersionCode());

        Map<String, Object> data = new HashMap<>();
        data.put("hasUpdate", hasUpdate);
        data.put("latestVersion", latest.getVersionCode());
        data.put("updateLog", latest.getUpdateLog());
        data.put("forceUpdate", latest.getForceUpdate());

        // 重点：由于你的实体类存的是 downloadUrl (可能是文件名如 "update.jar")
        // 我们返回给客户端一个相对 API 路径
        data.put("downloadUrl", "/api/version/download/" + latest.getDownloadUrl());

        return ResponseEntity.ok(new GlobalResponseBody("200", hasUpdate ? "发现新版本" : "已是最新版本", data));
    }

    /**
     * 文件下载接口
     */
    @LogOperation("文件下载")
    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        String updateDir = GlobalConfigHolder.getUpdateDir();
        try {
            Path path = Paths.get(updateDir).resolve(fileName).normalize();
            Resource resource = new UrlResource(path.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @LogOperation("上传新的安装包")
    @PostMapping("/upload")
    public ResponseEntity<GlobalResponseBody> uploadNewVersion(
            @RequestParam("file") MultipartFile file,
            @RequestParam("versionCode") String versionCode,
            @RequestParam("updateLog") String updateLog,
            @RequestParam("forceUpdate") Boolean forceUpdate) {

        // 1. 基础校验
        if (file.isEmpty()) {
            return ResponseEntity.ok(new GlobalResponseBody("400", "ERROR", "文件不能为空"));
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".zip")) {
            return ResponseEntity.ok(new GlobalResponseBody("400", "ERROR", "仅支持ZIP包上传"));
        }

        // 2. 调用 Service 处理业务
        try {
            String savedFile = versionService.publishNewVersion(file, versionCode, updateLog, forceUpdate);
            return ResponseEntity.ok(new GlobalResponseBody("200", "SUCCESS", "版本已发布: " + savedFile));
        } catch (IOException e) {
            return ResponseEntity.ok(new GlobalResponseBody("500", "ERROR", "文件IO异常: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(new GlobalResponseBody("500", "ERROR", "发布失败: " + e.getMessage()));
        }
    }

}
