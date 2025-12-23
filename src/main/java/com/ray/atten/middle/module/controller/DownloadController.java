package com.ray.atten.middle.module.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
public class DownloadController {

    // 从配置文件读取路径
    @Value("${atten_desktop.download.path}")
    private String downloadPath;

    private static final String EXE_NAME = "AttenDesktop.exe";

    @GetMapping("/api/download/desktop")
    public ResponseEntity<Resource> downloadFile() {
        try {
            // 1. 获取目录下的所有文件
            File dir = new File(downloadPath);
            if (!dir.exists() || !dir.isDirectory()) {
                log.error("下载目录不存在: {}", downloadPath);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            // 2. 筛选以 AttenDesktop 开头且以 .exe 结尾的文件
            File[] files = dir.listFiles((d, name) ->
                    name.toLowerCase().startsWith("attendesktop") && name.toLowerCase().endsWith(".exe")
            );

            if (files == null || files.length == 0) {
                log.warn("目录下未找到安装包: {}", downloadPath);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // 3. 按照最后修改时间排序，取最新的一个（防止目录里有旧版本）
            File latestFile = Arrays.stream(files)
                    .max(Comparator.comparingLong(File::lastModified))
                    .get();

            log.info("用户下载最新安装包: {}", latestFile.getName());

            // 4. 构建资源返回
            Path filePath = latestFile.toPath();
            Resource resource = new UrlResource(filePath.toUri());

            // 动态设置文件名，确保浏览器下载时保留原名（如 AttenDesktop_1.0.1.exe）
            String contentDisposition = "attachment; filename=\"" + latestFile.getName() + "\"";

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .body(resource);

        } catch (Exception e) {
            log.error("下载执行异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/download/latest-info")
    public ResponseEntity<Map<String, Object>> getLatestInfo() {
        File dir = new File(downloadPath);
        File[] files = dir.listFiles((d, name) ->
                name.toLowerCase().startsWith("attendesktop") && name.toLowerCase().endsWith(".exe"));

        if (files != null && files.length > 0) {
            File latestFile = Arrays.stream(files)
                    .max(Comparator.comparingLong(File::lastModified))
                    .get();

            Map<String, Object> info = new HashMap<>();
            String fileName = latestFile.getName();

            // 尝试从文件名提取版本号，如 AttenDesktop_1.0.1.exe 提取出 1.0.1
            String version = fileName.contains("-") ?
                    fileName.substring(fileName.indexOf("-") + 1, fileName.lastIndexOf(".")) :
                    "最新版";

            info.put("fileName", fileName);
            info.put("version", version);
            // 计算文件大小并转为 MB
            info.put("size", String.format("%.1f MB", latestFile.length() / (1024.0 * 1024.0)));
            info.put("updateTime", new java.text.SimpleDateFormat("yyyy-MM-dd").format(latestFile.lastModified()));

            return ResponseEntity.ok(info);
        }
        return ResponseEntity.notFound().build();
    }
}
