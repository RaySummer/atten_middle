package com.ray.atten.middle.module.service;

import com.ray.atten.middle.module.dto.AttendanceGroupDto;
import com.ray.atten.middle.module.dto.AttendanceGroupRequest;
import com.ray.atten.middle.module.model.AttendanceGroup;
import com.ray.atten.middle.module.model.Device;
import com.ray.atten.middle.module.repository.AttendanceGroupRepository;
import com.ray.atten.middle.module.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttendanceGroupService {

    @Autowired
    private AttendanceGroupRepository attendanceGroupRepository;
    @Autowired
    private DeviceRepository deviceRepository;

    public List<AttendanceGroupDto> queryAll() {
        List<AttendanceGroup> list = attendanceGroupRepository.findAll();
        if (list.isEmpty()) {
            return null;
        }
        List<AttendanceGroupDto> dtoList = new ArrayList<>();
        list.forEach(group -> {
            AttendanceGroupDto dto = new AttendanceGroupDto();
            dto.setUuid(group.getUuid());
            dto.setGroupName(group.getGroupName());
            List<String> sns = group.getDevices().stream().map(Device::getDeviceSn).collect(Collectors.toList());
            dto.setDeviceSns(sns);
            dtoList.add(dto);
        });
        return dtoList;
    }

    /**
     * 獲取所有考勤組 (列表，包含關聯設備信息)
     *
     * @return 考勤組列表
     */
    public List<AttendanceGroup> findAllGroups() {
        return attendanceGroupRepository.findAll();
    }

    /**
     * 【整合方法】創建或更新考勤組 (Upsert)
     * * @param request 包含 groupId (用於更新) 和 groupName, deviceSns 的請求 DTO
     *
     * @return 保存/更新後的實體
     */
    @Transactional
    public AttendanceGroup saveOrUpdateGroup(AttendanceGroupRequest request) {
        AttendanceGroup group;

        // --- 1. 判斷操作類型 (Create 還是 Update) ---
        if (request.getUuid() != null) {
            // Update: 查找現有實體
            group = attendanceGroupRepository.findByUuid(request.getUuid())
                    .orElseThrow(() -> new NoSuchElementException("找不到 ID 為 " + request.getGroupName() + " 的考勤組，無法更新。"));

            // 檢查 groupName 是否被其他組佔用
            attendanceGroupRepository.findByGroupName(request.getGroupName()).ifPresent(existing -> {
                if (!existing.getId().equals(group.getId())) {
                    throw new IllegalArgumentException("考勤組名稱已存在: " + request.getGroupName());
                }
            });

        } else {
            // Create: 實例化新實體
            group = new AttendanceGroup();

            // 檢查 groupName 是否已存在
            if (attendanceGroupRepository.findByGroupName(request.getGroupName()).isPresent()) {
                throw new IllegalArgumentException("考勤組名稱已存在: " + request.getGroupName());
            }
        }

        // --- 2. 設置基本屬性 ---
        group.setGroupName(request.getGroupName());

        // --- 3. 處理多對多關聯 (設備) ---
        Set<Device> managedDevices = mapSnsToDevices(request.getDeviceSns());
        group.setDevices(managedDevices); // JPA 會自動處理中間表的插入/刪除

        // --- 4. 保存並返回 ---
        return attendanceGroupRepository.save(group);
    }

    /**
     * 輔助方法：將設備序列號列表映射為已管理的 Device 實體集合。
     *
     * @param deviceSns 設備序列號列表
     * @return 設備實體集合
     */
    private Set<Device> mapSnsToDevices(List<String> deviceSns) {
        if (deviceSns == null || deviceSns.isEmpty()) {
            return null; // 返回空集合，清除所有關聯
        }

        // 批量查詢設備
        List<Device> devices = deviceRepository.findByDeviceSnIn(deviceSns);

        if (devices.size() != deviceSns.size()) {
            // 檢查是否有 SN 在數據庫中找不到
            Set<String> foundSns = devices.stream().map(Device::getDeviceSn).collect(Collectors.toSet());

            String missingSns = deviceSns.stream()
                    .filter(sn -> !foundSns.contains(sn))
                    .collect(Collectors.joining(", "));

            if (!missingSns.isEmpty()) {
                throw new NoSuchElementException("以下設備序列號無效或不存在: " + missingSns);
            }
        }

        return devices.stream().collect(Collectors.toSet());
    }


    /**
     * 根據 ID 查找考勤組
     *
     * @param id 考勤組 ID
     * @return 考勤組實體
     */
    public AttendanceGroup findGroupById(Long id) {
        return attendanceGroupRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("找不到 ID 為 " + id + " 的考勤組"));
    }

    /**
     * 刪除考勤組
     *
     * @param id 考勤組 ID
     */
    @Transactional
    public void deleteGroup(Long id) {
        AttendanceGroup group = attendanceGroupRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("找不到 ID 為 " + id + " 的考勤組"));

        // 刪除實體會自動刪除中間表中的所有關聯記錄
        attendanceGroupRepository.delete(group);
    }

}
