package com.ray.atten.middle.module.service;


import com.ray.atten.middle.module.dto.*;
import com.ray.atten.middle.module.model.EmployeeSyncQueue;
import com.ray.atten.middle.module.model.OaEmployee;
import com.ray.atten.middle.module.repository.EmployeeSyncQueueRepository;
import com.ray.atten.middle.module.repository.OaEmployeeRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OaEmployeeService {

    @Autowired
    private OaEmployeeRepository oaEmployeeRepository;
    @Autowired
    private EmployeeSyncQueueRepository employeeSyncQueueRepository;

    @Autowired
    private SyncDataService syncService;

    @Value("${employee.avater.photo}")
    private String folderPath;

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
            // 1. 核心修复：左连接 syncQueue，这样我们可以直接计算权重列
            Join<OaEmployee, EmployeeSyncQueue> syncJoin = root.join("syncQueue", JoinType.LEFT);

            List<Predicate> predicates = new ArrayList<>();

            // --- 基础筛选逻辑 ---
            if (StringUtils.isNotEmpty(request.getKeyword())) {
                String likePattern = "%" + request.getKeyword().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("pin")), likePattern),
                        cb.like(cb.lower(root.get("name")), likePattern)
                ));
            }
            if (request.getInService() != null) {
                predicates.add(cb.equal(root.get("inService"), request.getInService()));
            }

            // --- 排序逻辑 ---
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {

                // 1. 离职/在职 排序权重：在职(true/1)设为 0，离职(false/0)设为 1
                // 升序(ASC)排序后，在职(0)在前，离职(1)在后
                Expression<Integer> inServicePoint = cb.selectCase()
                        .when(cb.equal(root.get("inService"), true), 0) // 如果实体类是 Integer/Byte，换成对应的 1 或 0
                        .otherwise(1)
                        .as(Integer.class);

                // 2. 定义指纹和照片的单行权重
                Expression<Integer> fingerPoint = cb.selectCase()
                        .when(cb.and(
                                cb.equal(syncJoin.get("type"), "finger"),
                                cb.isNotNull(syncJoin.get("base64Data")),
                                cb.greaterThan(cb.length(syncJoin.get("base64Data")), 10)
                        ), 1).otherwise(0).as(Integer.class);

                Expression<Integer> photoPoint = cb.selectCase()
                        .when(cb.and(
                                cb.equal(syncJoin.get("type"), "photo"),
                                cb.isNotNull(syncJoin.get("base64Data")),
                                cb.greaterThan(cb.length(syncJoin.get("base64Data")), 10)
                        ), 1).otherwise(0).as(Integer.class);

                // 关键：必须分组，否则 Join 会导致数据重复
                query.groupBy(root.get("id"));

                // 排序规则说明：
                // 1. inServicePoint ASC -> 在职在前，离职在后
                // 2. fingerPoint ASC     -> 未录指纹在前
                // 3. photoPoint ASC      -> 未录照片在前
                // 4. createTime DESC     -> 最新创建在前
                query.orderBy(
//                        cb.asc(inServicePoint),
//                        cb.asc(cb.max(fingerPoint)),
//                        cb.asc(cb.max(photoPoint)),
                        cb.desc(root.get("createTime"))
                );
            }

            // --- 过滤逻辑 (使用原始 Subquery 确保准确性) ---
            if (request.getHasFingerprint() != null) {
                Subquery<Integer> fpSub = query.subquery(Integer.class);
                Root<EmployeeSyncQueue> fpRoot = fpSub.from(EmployeeSyncQueue.class);
                fpSub.select(cb.literal(1)).where(
                        cb.equal(fpRoot.get("pin"), root.get("pin")),
                        cb.equal(fpRoot.get("type"), "finger"),
                        cb.isNotNull(fpRoot.get("base64Data")),
                        cb.greaterThan(cb.length(fpRoot.get("base64Data")), 10)
                );
                predicates.add(request.getHasFingerprint() ? cb.exists(fpSub) : cb.not(cb.exists(fpSub)));
            }

            if (request.getHasPhoto() != null) {
                Subquery<Integer> phSub = query.subquery(Integer.class);
                Root<EmployeeSyncQueue> phRoot = phSub.from(EmployeeSyncQueue.class);
                phSub.select(cb.literal(1)).where(
                        cb.equal(phRoot.get("pin"), root.get("pin")),
                        cb.equal(phRoot.get("type"), "photo"),
                        cb.isNotNull(phRoot.get("base64Data")),
                        cb.greaterThan(cb.length(phRoot.get("base64Data")), 10)
                );
                predicates.add(request.getHasPhoto() ? cb.exists(phSub) : cb.not(cb.exists(phSub)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return oaEmployeeRepository.findAll(spec, pageable).map(OaEmployeeDto::convertToDto);
    }

    /**
     * 輔助方法：根據請求參數構造 Sort 對象，並校驗字段的安全性。
     */
    private Sort createSort(String sortBy, String sortOrder) {
        if (!StringUtils.isNotEmpty(sortBy) || !ALLOWED_SORT_FIELDS.contains(sortBy)) {
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

    @Transactional(readOnly = true)
    public List<OaEmployeeDto> findAllEmployee() {
        return oaEmployeeRepository.findAll().stream().map(OaEmployeeDto::convertToDto).collect(Collectors.toList());
    }

    @Transactional
    public void saveSyncEmployeeDataList(List<EmployeeSyncRequest> requestList) {
        List<EmployeeSyncQueue> syncQueues = new ArrayList<>();
        for (EmployeeSyncRequest request : requestList) {
            syncQueues.addAll(saveSyncEmployeeData(request));
        }

        if (!syncQueues.isEmpty()) {
            syncService.syncUserToDevices(syncQueues);
        }
    }

    @Transactional
    public List<EmployeeSyncQueue> saveSyncEmployeeData(EmployeeSyncRequest request) {
        List<EmployeeSyncQueue> syncQueues = new ArrayList<>();
        EmployeeSyncQueue syncQueue;

        if (request.getFingerFidList() == null || request.getFingerFidList().isEmpty()) {
            syncQueue = employeeSyncQueueRepository.findByPinAndFid(request.getPin(), 0);
            if (syncQueue == null) {
                syncQueue = new EmployeeSyncQueue();
            }
            syncQueue.setPin(request.getPin());
            syncQueue.setName(request.getName());
            syncQueue.setPri(request.getPri());
            syncQueue.setVerify(request.getVerify());
            syncQueue.setValid(request.getValid());
            syncQueue.setFid(0);
            syncQueue.setTargetDeviceSn(request.getDeviceSn());
            syncQueue.setStatus(0);

            employeeSyncQueueRepository.save(syncQueue);
            syncQueues.add(syncQueue);
        } else {
            for (EmployeeFingerFidDto dto : request.getFingerFidList()) {
                if (StringUtils.isNotEmpty(dto.getType()) && dto.getType().equals("photo")) {
                    syncQueue = employeeSyncQueueRepository.findByPinAndType(request.getPin(), dto.getType());
                } else {
                    syncQueue = employeeSyncQueueRepository.findByPinAndFid(request.getPin(), dto.getFid());
                }
                if (syncQueue == null) {
                    syncQueue = new EmployeeSyncQueue();
                }
                if (StringUtils.isNotEmpty(dto.getType())) {
                    syncQueue.setPin(request.getPin());
                    syncQueue.setName(request.getName());
                    syncQueue.setPri(request.getPri());
                    syncQueue.setVerify(request.getVerify());
                    syncQueue.setValid(request.getValid());
                    syncQueue.setTargetDeviceSn(request.getDeviceSn());
                    syncQueue.setPasswd(request.getPasswd());
                    syncQueue.setStatus(0);
                    syncQueue.setFid(dto.getFid());
                    syncQueue.setBase64Data(dto.getBase64Data());
                    syncQueue.setBase64Size(dto.getBase64Data().trim().length());
                    syncQueue.setType(dto.getType());

                    employeeSyncQueueRepository.save(syncQueue);
                    syncQueues.add(syncQueue);
                }
            }
        }

        Optional<OaEmployee> optional = oaEmployeeRepository.findByPin(request.getPin());
        if (optional.isPresent()) {
            OaEmployee oaEmployee = optional.get();
            oaEmployee.setAvatar(request.getPhotoBase64());

            oaEmployeeRepository.save(oaEmployee);
        }
        return syncQueues;
    }

    /**
     * 批量从文件夹更新员工头像
     *
     */
    @Transactional
    public void batchUpdatePhotosFromFolder() {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            log.error("指定的路径不是有效的文件夹: {}", folderPath);
            return;
        }

        File[] files = folder.listFiles((dir, name) -> {
            String lowerName = name.toLowerCase();
            return lowerName.endsWith(".jpg") || lowerName.endsWith(".png") || lowerName.endsWith(".jpeg");
        });

        if (files == null || files.length == 0) {
            log.warn("文件夹中未找到图片文件: {}", folderPath);
            return;
        }

        int successCount = 0;
        for (File file : files) {
            try {
                // 1. 获取文件名（不带后缀）作为 PIN
                String fileName = file.getName();
                String pin = fileName.substring(0, fileName.lastIndexOf("."));

                // 2. 根据 PIN 查询员工
                Optional<OaEmployee> employeeOpt = oaEmployeeRepository.findByPin(pin);

                if (employeeOpt.isPresent()) {
                    OaEmployee employee = employeeOpt.get();

                    // 3. 读取图片并转换为 Base64
                    byte[] fileContent = Files.readAllBytes(file.toPath());
                    String base64Image = Base64.getEncoder().encodeToString(fileContent);

                    // 假设头像存在 avatar 字段中（根据你的实体类定义）
                    // 如果 SDK 需要 DataURI 格式，可以加上: "data:image/jpeg;base64,"
                    employee.setAvatar(base64Image);

                    oaEmployeeRepository.save(employee);
                    successCount++;
                    log.info("成功更新员工照片: PIN = {}, 文件名 = {}", pin, fileName);
                } else {
                    log.warn("未找到对应员工: PIN = {}, 文件名 = {}", pin, fileName);
                }
            } catch (IOException e) {
                log.error("处理文件失败: {}, 错误: {}", file.getName(), e.getMessage());
            }
        }
        log.info("批量任务完成，成功更新 {} 名员工的照片。", successCount);
    }

}
