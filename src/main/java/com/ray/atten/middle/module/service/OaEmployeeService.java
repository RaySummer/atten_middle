package com.ray.atten.middle.module.service;


import com.ray.atten.middle.module.dto.OaEmployeeDto;
import com.ray.atten.middle.module.dto.OaEmployeeQueryPageRequest;
import com.ray.atten.middle.module.dto.OaEmployeeRequest;
import com.ray.atten.middle.module.model.EmployeeSyncQueue;
import com.ray.atten.middle.module.model.OaEmployee;
import com.ray.atten.middle.module.repository.EmployeeSyncQueueRepository;
import com.ray.atten.middle.module.repository.OaEmployeeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
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

            // C. 指纹/照片筛选
            if (request.getHasFingerprint() != null) {
                // 判定逻辑统一使用 coalesce 防止 NULL 导致过滤失效
                Expression<Integer> len = cb.length(cb.coalesce(syncJoin.get("fingerprint"), ""));
                predicates.add(request.getHasFingerprint() ? cb.greaterThan(len, 10) : cb.lessThanOrEqualTo(len, 10));
            }
            if (request.getHasPhoto() != null) {
                Expression<Integer> len = cb.length(cb.coalesce(syncJoin.get("photoBase64"), ""));
                predicates.add(request.getHasPhoto() ? cb.greaterThan(len, 10) : cb.lessThanOrEqualTo(len, 10));
            }

            // --- 排序逻辑 (仅在数据查询时注入) ---
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {

                // 使用 coalesce 确保即使 syncJoin 关联不到数据，长度也会被当做 0 处理
                Expression<Integer> fpLen = cb.length(cb.coalesce(syncJoin.get("fingerprint"), ""));
                Expression<Integer> photoLen = cb.length(cb.coalesce(syncJoin.get("photoBase64"), ""));

                // 只有 (指纹 < 10) 且 (照片 < 10) 才是优先级 0 (最优先)
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
   /* @Transactional(readOnly = true) // 确保查询是只读的
    public Page<OaEmployeeDto> queryEmployees(OaEmployeeQueryPageRequest request) {

        // --- 第 1 步: 执行主查询 (OaEmployee) ---

        // 1. 處理排序邏輯 (保持不变)
        Sort sort = createSort(request.getSortBy(), request.getSortOrder());

        // 1. 構造分頁對象 (Pageable) (保持不变)
        Pageable pageable = PageRequest.of(
                request.getPageNum() - 1,
                request.getPageSize(),
                sort
        );

        // 2. 構造查詢規範 (Specification) (保持不变)
        Specification<OaEmployee> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // A. 模糊查詢 (PIN 或 Name 模糊查詢)
            if (StringUtils.hasText(request.getKeyword())) {
                String likePattern = "%" + request.getKeyword() + "%";
                Predicate keywordPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("pin")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), likePattern)
                );
                predicates.add(keywordPredicate);
            }

            // D. 在職狀態查詢
            if (request.getInService() != null) {
                predicates.add(criteriaBuilder.equal(root.get("inService"), request.getInService()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        // 3. 執行查詢並返回 Page<OaEmployee> 對象
        Page<OaEmployee> oaEmployeePage = oaEmployeeRepository.findAll(spec, pageable);


        // --- 第 2 步 & 第 3 步: 提取 PIN 并查询关联数据 ---

        // 如果没有数据，直接返回空分页
        if (oaEmployeePage.isEmpty()) {
            return new PageImpl<>(
                    new ArrayList<>(),
                    pageable,
                    0
            );
        }

        // 提取当前页所有 OaEmployee 的 pin 列表
        List<String> pins = oaEmployeePage.getContent().stream()
                .map(OaEmployee::getPin)
                .collect(Collectors.toList());

        // 根据 pin 列表批量查询 EmployeeSyncQueue 数据
        List<EmployeeSyncQueue> syncQueueList = employeeSyncQueueRepository.findByPinIn(pins);

        // 将 EmployeeSyncQueue 列表转换为 Map<Pin, EmployeeSyncQueue>，便于快速查找
        Map<String, EmployeeSyncQueue> syncQueueMap = syncQueueList.stream()
                .collect(Collectors.toMap(EmployeeSyncQueue::getPin, sq -> sq));


        // --- 第 4 步: 数据映射与组装 (OaEmployee -> OaEmployeeDto) ---

        List<OaEmployeeDto> dtoList = oaEmployeePage.getContent().stream()
                .map(oaEmployee -> {
                    // 初始化 OaEmployeeDto，从 OaEmployee 复制基础字段
                    OaEmployeeDto dto = convertToDto(oaEmployee);

                    // 从 Map 中查找对应的 EmployeeSyncQueue 数据
                    EmployeeSyncQueue syncData = syncQueueMap.get(oaEmployee.getPin());

                    // 如果找到关联数据，则设置 DTO 中缺少的字段
                    if (syncData != null) {
                        dto.setFingerprint(syncData.getFingerprint());
                        dto.setPhotoBase64(syncData.getPhotoBase64());
                        dto.setFid(syncData.getFid()); // 假设 EmployeeSyncQueue 有对应的 getter
                        dto.setFingerSize(syncData.getFingerSize());
                        dto.setPhotoSize(syncData.getPhotoSize());
                        // ... 其他字段
                    }
                    return dto;
                })
                .collect(Collectors.toList());


        // --- 第 5 步: 构建新的 Page 对象并返回 ---

        // 使用组装好的 DTO 列表、原始分页信息和总记录数构建 PageImpl
        return new PageImpl<>(
                dtoList,
                pageable,
                oaEmployeePage.getTotalElements() // 使用原始的总记录数
        );
    }*/

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


}
