package com.ray.atten.middle.module.service;


import com.ray.atten.middle.module.dto.*;
import com.ray.atten.middle.module.model.EmployeeSyncQueue;
import com.ray.atten.middle.module.model.OaEmployee;
import com.ray.atten.middle.module.repository.EmployeeSyncQueueRepository;
import com.ray.atten.middle.module.repository.OaEmployeeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OaEmployeeService {

    @Autowired
    private OaEmployeeRepository oaEmployeeRepository;
    @Autowired
    private EmployeeSyncQueueRepository employeeSyncQueueRepository;

    // 定義允許排序的字段列表（與實體類的屬性名稱一致）
    private static final Set<String> ALLOWED_SORT_FIELDS = new HashSet<>(Arrays.asList(
            "id",
            "pin",
            "inService",
            "entryDate",
            "createTime"
    ));

    /**
     * 查询员工列表并组装 EmployeeSyncQueue 的数据
     *
     * @param request 查询请求
     * @return 包含 OaEmployeeDto 的分页结果
     */
    @Transactional(readOnly = true)
    public Page<OaEmployeeDto> queryEmployees(OaEmployeeQueryPageRequest request) {

        Pageable pageable = PageRequest.of(request.getPageNum() - 1, request.getPageSize());

        Specification<OaEmployee> spec = (root, query, cb) -> {
            // 1. 必须使用 LEFT JOIN，确保即使 syncQueue 没记录，员工也能查出来
            Join<OaEmployee, EmployeeSyncQueue> syncJoin = root.join("syncQueue", JoinType.LEFT);

            List<Predicate> predicates = new ArrayList<>();

            // --- 过滤逻辑 ---

            // A. 关键字 (PIN/Name)
            if (StringUtils.hasText(request.getKeyword())) {
                String likePattern = "%" + request.getKeyword().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("pin")), likePattern),
                        cb.like(cb.lower(root.get("name")), likePattern)
                ));
            }

            // B. 在职状态 (关键点：只有不为 null 时才加条件，为 null 时查全部)
            if (request.getInService() != null) {
                predicates.add(cb.equal(root.get("inService"), request.getInService()));
            }

            // C. 指纹筛选逻辑修正
            if (request.getHasFingerprint() != null) {
                // 统一处理逻辑：先 coalesce 转为空串，再算长度
                Expression<Integer> fpLen = cb.length(cb.coalesce(syncJoin.get("fingerprint"), ""));

                if (request.getHasFingerprint()) {
                    predicates.add(cb.greaterThan(fpLen, 10));
                } else {
                    // 无指纹：长度 <= 10
                    predicates.add(cb.lessThanOrEqualTo(fpLen, 10));
                }
            }

            // --- 排序逻辑 ---
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                // 依然使用 coalesce 确保 NULL 安全
                Expression<Integer> fpLen = cb.length(cb.coalesce(syncJoin.get("fingerprint"), ""));
                Expression<Integer> photoLen = cb.length(cb.coalesce(syncJoin.get("photoBase64"), ""));

                // 严谨的 Priority 定义
                Expression<Integer> priority = cb.selectCase()
                        .when(cb.and(cb.lessThan(fpLen, 10), cb.lessThan(photoLen, 10)), 0)
                        .otherwise(1)
                        .as(Integer.class);

                query.orderBy(cb.asc(priority), cb.desc(root.get("entryDate")));
            }

            // 如果没有选任何条件，cb.and(...) 会生成一个 1=1 的条件
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 执行查询
        return oaEmployeeRepository.findAll(spec, pageable).map(oaEmployee -> {
            OaEmployeeDto dto = convertToDto(oaEmployee);
            EmployeeSyncQueue syncData = oaEmployee.getSyncQueue();
            if (syncData != null) {
                dto.setFingerprint(syncData.getFingerprint());
                dto.setPhotoBase64(syncData.getPhotoBase64());
            }
            return dto;
        });
    }

    // 辅助方法：将 OaEmployee 转换为 OaEmployeeDto（基础字段）
    private OaEmployeeDto convertToDto(OaEmployee oaEmployee) {
        OaEmployeeDto dto = new OaEmployeeDto();
        dto.setUuid(oaEmployee.getUuid());
        dto.setPin(oaEmployee.getPin());
        dto.setName(oaEmployee.getName());
        dto.setCompany(oaEmployee.getCompany());
        dto.setDept(oaEmployee.getDept());
        dto.setInService(oaEmployee.getInService());
        dto.setEntryDate(oaEmployee.getEntryDate());
        dto.setCreateTime(oaEmployee.getCreateTime());
        dto.setOfficeLocation(oaEmployee.getOfficeLocation());
        return dto;
    }

    /**
     * 輔助方法：根據請求參數構造 Sort 對象，並校驗字段的安全性。
     */
    private Sort createSort(String sortBy, String sortOrder) {
        if (!StringUtils.hasText(sortBy) || !ALLOWED_SORT_FIELDS.contains(sortBy)) {
            // 如果未指定排序字段或字段不合法，則使用默認排序
            return Sort.by("createTime").descending(); // 默認按創建時間降序
        }

        // 判斷排序方向 (ASC/DESC)
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortOrder) ?
                Sort.Direction.ASC :
                Sort.Direction.DESC;

        // 構造 Sort 對象
        return Sort.by(direction, sortBy);
    }

    /**
     * 異步批量保存或更新 (Upsert) 員工數據。
     * 使用 @Async 註解，並指定專門的線程池。
     */
    @Async("employeePushExecutor") // 指定使用我們在 AsyncConfig 中定義的線程池
    @Transactional // 確保整個批量操作在單個事務中
    public void asyncBatchUpsert(List<OaEmployeeRequest> employees) {
        if (employees == null || employees.isEmpty()) {
            return;
        }

        // 1. 批量查詢現有的 PIN 碼
        Set<String> pushedPins = employees.stream()
                .map(OaEmployeeRequest::getPin)
                .collect(Collectors.toSet());

        // 創建一個只包含 PIN 碼和 ID 的投影 (Projection) 來減少數據庫壓力
        // 假設 Repository 有一個方法 findByPinIn(Set<String> pins)
        // Spring Data JPA 通常需要自定義查詢來實現這個優化。

        // --- 優化點：先查出所有已存在的 PINs ---
        List<OaEmployee> existingEmployees = oaEmployeeRepository.findByPinIn(pushedPins);

        // 將現有數據轉換為 Map，以 PIN 為鍵，方便 O(1) 時間查找
        Map<String, OaEmployee> existingMap = existingEmployees.stream()
                .collect(Collectors.toMap(OaEmployee::getPin, e -> e));

        // 2. 分離新增和更新列表
        List<OaEmployee> toInsert = new ArrayList<>();
        List<OaEmployee> toUpdate = new ArrayList<>();

        for (OaEmployeeRequest pushedEmployee : employees) {
            OaEmployee existing = existingMap.get(pushedEmployee.getPin());

            if (existing != null) {
                // 這裡應該包含一個細緻的業務邏輯，例如只更新部分字段
                toUpdate.add(convertTo(existing, pushedEmployee));
            } else {
                // 新增操作
                toInsert.add(convertTo(null, pushedEmployee));
            }
        }

        // 3. 執行批量操作 (利用 JDBC Batching)
        // --- 批量新增 ---
        oaEmployeeRepository.saveAllAndFlush(toInsert);

        // --- 批量更新 ---
        oaEmployeeRepository.saveAllAndFlush(toUpdate);

        log.debug("Batch Upsert completed. Inserted: " + toInsert.size() + ", Updated: " + toUpdate.size());
    }

    // 輔助方法：根據業務規則更新字段
    private void updateEmployeeFields(OaEmployee existing, OaEmployee pushed) {
        // 這裡只更新推送到字段，如 name, company, dept, inService, updateTime
        existing.setName(pushed.getName());
        existing.setCompany(pushed.getCompany());
        existing.setDept(pushed.getDept());
        existing.setInService(pushed.getInService());
    }

    private OaEmployee convertTo(OaEmployee existing, OaEmployeeRequest request) {
        if (existing == null) {
            existing = new OaEmployee();
        }
        existing.setName(request.getName());
        existing.setCompany(request.getCompany());
        existing.setDept(request.getDept());
        existing.setOfficeLocation(request.getOfficeLocation());
        existing.setEntryDate(request.getEntryDate());
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(request.getInService()) && !request.getInService().equals("离职")) {
            existing.setInService(Boolean.TRUE);
        } else {
            existing.setInService(Boolean.FALSE);
        }

        return existing;
    }

    public List<OaEmployeeDto> findAllEmployee() {
        return oaEmployeeRepository.findAll().stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Transactional
    public void saveSyncEmployeeData(EmployeeSyncRequest request) {
        EmployeeSyncQueue syncQueue;
        syncQueue = employeeSyncQueueRepository.findByPin(request.getPin());
        if (syncQueue == null) {
            syncQueue = new EmployeeSyncQueue();
        }
        syncQueue.setPin(request.getPin());
        syncQueue.setName(request.getName());
        syncQueue.setPri(request.getPri());
        syncQueue.setFingerprint(request.getFingerprint());
        syncQueue.setVerify(request.getVerify());
        syncQueue.setValid(request.getValid());
        syncQueue.setFid(request.getFid());
        syncQueue.setTargetDeviceSn(request.getDeviceSn());
        syncQueue.setPhotoBase64(request.getPhotoBase64());
        syncQueue.setFingerSize(request.getFingerSize());
        syncQueue.setPhotoSize(request.getPhotoSize());
        syncQueue.setPasswd(request.getPasswd());
        syncQueue.setStatus(0);

        Optional<OaEmployee> optional = oaEmployeeRepository.findByPin(request.getPin());
        if (optional.isPresent()) {
            OaEmployee oaEmployee = optional.get();
            oaEmployee.setAvatar(request.getPhotoBase64());

            oaEmployeeRepository.save(oaEmployee);
        }

        employeeSyncQueueRepository.save(syncQueue);
    }

}
