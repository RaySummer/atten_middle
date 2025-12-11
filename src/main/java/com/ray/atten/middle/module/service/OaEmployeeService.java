package com.ray.atten.middle.module.service;


import com.ray.atten.middle.module.dto.OaEmployeeQueryRequest;
import com.ray.atten.middle.module.model.OaEmployee;
import com.ray.atten.middle.module.repository.OaEmployeeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OaEmployeeService {

    @Autowired
    private OaEmployeeRepository oaEmployeeRepository;

    // 定義允許排序的字段列表（與實體類的屬性名稱一致）
    private static final Set<String> ALLOWED_SORT_FIELDS = new HashSet<>(Arrays.asList(
            "id",
            "pin",
            "inService",
            "entryDate",
            "createTime"
    ));

    public Page<OaEmployee> queryEmployees(OaEmployeeQueryRequest request) {

        // 1. 處理排序邏輯
        Sort sort = createSort(request.getSortBy(), request.getSortOrder());

        // 1. 構造分頁對象 (Pageable)
        // JPA 的分頁從索引 0 開始，所以頁碼需要調整
        Pageable pageable = PageRequest.of(
                request.getPageNum() - 1,
                request.getPageSize(),
                sort // 將構造好的 Sort 對象傳入 PageRequest
        );

        // 2. 構造查詢規範 (Specification)
        Specification<OaEmployee> spec = (root, query, criteriaBuilder) -> {

            // 用於存儲所有 AND 條件的列表
            List<Predicate> predicates = new ArrayList<>();

            // A. 模糊查詢 (PIN 或 Name 模糊查詢)
            if (StringUtils.hasText(request.getKeyword())) {
                String likePattern = "%" + request.getKeyword().toLowerCase() + "%";

                // 構造一個 OR 條件: WHERE LOWER(pin) LIKE ? OR LOWER(name) LIKE ?
                Predicate keywordPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("pin")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), likePattern)
                );
                predicates.add(keywordPredicate);
            }

//            // B. 級聯查詢 - 分公司 (未來擴展)
//            if (StringUtils.hasText(request.getCompanyId())) {
//                predicates.add(criteriaBuilder.equal(root.get("company"), request.getCompanyId()));
//            }
//
//            // C. 級聯查詢 - 部門 (未來擴展)
//            if (StringUtils.hasText(request.getDeptId())) {
//                predicates.add(criteriaBuilder.equal(root.get("dept"), request.getDeptId()));
//            }
//
//            // D. 在職狀態查詢
//            if (request.getInService() != null) {
//                predicates.add(criteriaBuilder.equal(root.get("inService"), request.getInService()));
//            }

            // 將所有條件組合起來 (使用 AND 連接)
            // criteriaBuilder.and(predicates.toArray(new Predicate[0]))
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        // 3. 執行查詢並返回 Page 對象 (包含數據列表和總記錄數)
        return oaEmployeeRepository.findAll(spec, pageable);
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
    public void asyncBatchUpsert(List<OaEmployee> employees) {
        if (employees == null || employees.isEmpty()) {
            return;
        }

        // 1. 批量查詢現有的 PIN 碼
        Set<String> pushedPins = employees.stream()
                .map(OaEmployee::getPin)
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

        for (OaEmployee pushedEmployee : employees) {
            OaEmployee existing = existingMap.get(pushedEmployee.getPin());

            if (existing != null) {
                // 更新操作：設置現有的 ID，並更新其他字段
                pushedEmployee.setId(existing.getId());
                // 這裡應該包含一個細緻的業務邏輯，例如只更新部分字段
                updateEmployeeFields(existing, pushedEmployee); // 假設有輔助方法
                toUpdate.add(existing);
            } else {
                // 新增操作
                toInsert.add(pushedEmployee);
            }
        }

        // 3. 執行批量操作 (利用 JDBC Batching)
        // --- 批量新增 ---
        oaEmployeeRepository.saveAll(toInsert);

        // --- 批量更新 ---
        oaEmployeeRepository.saveAll(toUpdate);

        // 4. 確保事務提交並清除 JPA Session 緩存 (重要)
        // 在數據量大時，需要確保 Session 不會累積過多實體
        // 由於我們使用 @Transactional，可以在這裡手動刷新/清理
        // entityManager.flush(); // 如果使用 EntityManager
        // entityManager.clear();

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

}
