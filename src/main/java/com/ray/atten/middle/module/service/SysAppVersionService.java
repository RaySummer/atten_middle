package com.ray.atten.middle.module.service;

import com.ray.atten.middle.module.model.SysAppVersion;
import com.ray.atten.middle.module.repository.SysAppVersionRepository;
import com.ray.atten.middle.module.utils.GlobalConfigHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class SysAppVersionService {

    private static final long MAX_ZIP_SIZE = 500 * 1024 * 1024; // 500MB 防御解压炸弹

    @Autowired
    private SysAppVersionRepository versionRepository;

    /**
     * 获取数据库中最新的一条版本发布记录
     */
    public SysAppVersion getLatestVersion() {
        return versionRepository.findFirstByOrderByCreateTimeDesc();
    }

    /**
     * 版本比对逻辑
     *
     * @param currentVersion 客户端传来的版本号，如 "1.0.0"
     * @param latestVersion  数据库最新的版本号，如 "1.1.0"
     * @return true 表示有新版本
     */
    public boolean isNewer(String currentVersion, String latestVersion) {
        if (currentVersion == null || latestVersion == null) return false;

        String[] currParts = currentVersion.split("\\.");
        String[] lateParts = latestVersion.split("\\.");

        int length = Math.max(currParts.length, lateParts.length);

        for (int i = 0; i < length; i++) {
            int curr = i < currParts.length ? Integer.parseInt(currParts[i]) : 0;
            int late = i < lateParts.length ? Integer.parseInt(lateParts[i]) : 0;
            if (late > curr) return true;
            if (late < curr) return false;
        }
        return false;
    }

    @Transactional(rollbackFor = Exception.class)
    public String publishNewVersion(MultipartFile file, String versionCode, String updateLog, Boolean forceUpdate) throws Exception {
        // 1. 获取目标目录（从内存配置持有者获取）
        String updateDir = GlobalConfigHolder.getUpdateDir();
        File targetFolder = new File(updateDir);
        if (!targetFolder.exists() && !targetFolder.mkdirs()) {
            throw new IOException("无法创建存储目录: " + updateDir);
        }

        String savedJarName = null;
        long totalUnzippedSize = 0;

        // 2. 解压并提取 .jar
        try (ZipInputStream zis = new ZipInputStream(file.getInputStream(), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];

            while ((entry = zis.getNextEntry()) != null) {
                // 防御 Zip Slip: 仅取文件名部分
                String entryName = new File(entry.getName()).getName();

                if (!entry.isDirectory() && entryName.toLowerCase().endsWith(".jar")) {
                    // 构造存储文件名：固定格式方便管理，如 AttenDesktop_1_1_0.jar
                    savedJarName = "AttenDesktop_" + versionCode.replace(".", "_") + ".jar";
                    File targetFile = new File(targetFolder, savedJarName);

                    // 二次校验路径安全
                    if (!targetFile.getCanonicalPath().startsWith(targetFolder.getCanonicalPath())) {
                        throw new IOException("检测到非法解压路径: " + entryName);
                    }

                    try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            totalUnzippedSize += len;
                            if (totalUnzippedSize > MAX_ZIP_SIZE) {
                                throw new IOException("解压体积超出安全限制");
                            }
                            fos.write(buffer, 0, len);
                        }
                    }
                    zis.closeEntry();
                    break; // 提取到第一个 jar 后退出
                }
                zis.closeEntry();
            }
        }

        if (savedJarName == null) {
            throw new RuntimeException("ZIP包中未检测到有效的 .jar 文件");
        }

        // 3. 数据库持久化
        SysAppVersion newVersion = new SysAppVersion();
        newVersion.setVersionCode(versionCode);
        newVersion.setUpdateLog(updateLog);
        newVersion.setForceUpdate(forceUpdate);
        newVersion.setDownloadUrl(savedJarName); // 存储文件名供下载接口使用

        versionRepository.save(newVersion);

        return savedJarName;
    }
}
