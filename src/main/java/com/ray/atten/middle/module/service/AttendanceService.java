package com.ray.atten.middle.module.service;

import com.ray.atten.middle.module.dto.AttendanceLogDto;
import com.ray.atten.middle.module.dto.AttendanceLogRequest;
import com.ray.atten.middle.module.model.AttendanceLog;
import com.ray.atten.middle.module.repository.AttendanceLogRepository;
import com.ray.atten.middle.module.utils.AttendanceMqSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AttendanceService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private AttendanceLogRepository logRepo;
    @Autowired
    private AttendanceMqSender mqSender;

    @Transactional
    public List<AttendanceLog> processAttLogData(String sn, String bodyData) {
        log.debug("Processing ATTLOG data from: " + sn);
        List<AttendanceLog> logs = new ArrayList<>();

        // 1. 按行分割数据
        String[] lines = bodyData.trim().split("\n");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        for (String line : lines) {
            if (line.trim().isEmpty()) continue;

            // 数据是 Tab 分隔的：
            // 101021	2025-11-22 12:01:16	0	15	0	0	0	255	0	0
            // verifyType = 15(人脸) 0(指静脉或人脸或指纹或卡或密码) 1(仅指纹)
            // STATUS:0-成功，1-失败，2-黑名单
            String[] fields = line.split("\t");

            if (fields.length >= 4) { // 至少应有 PIN, Time, VerifyType, Status
                String verifyTime = fields[1].trim();
                LocalDateTime verifiedTime = parseDateTime(verifyTime, false);
                AttendanceLog attendanceLog = logRepo.findByDeviceSnAndUserPinAndVerifyTime(sn, fields[0].trim(), verifiedTime);
                if (attendanceLog == null) {
                    attendanceLog = new AttendanceLog();
                    attendanceLog.setDeviceSn(sn);
                    attendanceLog.setUserPin(fields[0].trim());

                    attendanceLog.setVerifyTime(verifiedTime);
                    attendanceLog.setStatus(Integer.valueOf(fields[2].trim()));
                    attendanceLog.setVerifyType(Integer.valueOf(fields[3].trim()));

                    logRepo.save(attendanceLog);
                    logs.add(attendanceLog);
                    log.debug("Saved AttLog: " + attendanceLog.getUserPin() + " " + attendanceLog.getVerifyTime());
//
//                    try {
//                        // 在接收到推送的方法内：
//                        Map<String, Object> data = new HashMap<>();
//                        data.put("empid", attendanceLog.getUserPin());
//                        data.put("dktime", attendanceLog.getVerifyTime());
//                        data.put("clocksno", attendanceLog.getDeviceSn());
//
//                        mqSender.sendSyncMessage(data);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
                }
                log.debug("This AttLog Exists " + attendanceLog.getUserPin() + " " + attendanceLog.getVerifyTime());
            }

        }

        return logs;
    }

    public Page<AttendanceLogDto> findLogPage(AttendanceLogRequest request) {
        // 1. 处理排序方向：确保不为空且格式正确
        Sort.Direction direction = Sort.Direction.DESC; // 默认降序
        if (StringUtils.hasText(request.getSortOrder())) {
            try {
                // 使用 fromString 转换，失败则捕获异常保持默认值
                direction = Sort.Direction.fromString(request.getSortOrder().trim());
            } catch (IllegalArgumentException e) {
                // 记录日志或直接回退到默认值
                direction = Sort.Direction.DESC;
            }
        }

        // 2. 处理排序字段
        String sortBy = StringUtils.hasText(request.getSortBy()) ? request.getSortBy() : "verifyTime";

        // 3. 构建 Pageable
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(request.getPageNum() - 1, request.getPageSize(), sort);
        // 2. 构建动态查询 Specification
        Specification<AttendanceLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // --- A. Keyword 模糊匹配 (PIN or SN or Name) ---
            if (StringUtils.hasText(request.getKeyword())) {
                String pattern = "%" + request.getKeyword() + "%";

                // 注意：Keyword 查询必须是 OR 关系
                Predicate pinPredicate = cb.like(root.get("userPin"), pattern);
                Predicate snPredicate = cb.like(root.get("deviceSn"), pattern);
                // 关联员工表的姓名
                Predicate namePredicate = cb.like(root.join("employee", JoinType.LEFT).get("name"), pattern);

                predicates.add(cb.or(pinPredicate, snPredicate, namePredicate));
            }

            // --- B. 时间范围处理 (默认值逻辑) ---
            LocalDateTime start;
            if (StringUtils.hasText(request.getStartTime())) {
                start = parseDateTime(request.getStartTime(), false);
            } else {
                // 默认一星期前 (本周一 00:00:00 或简单的当前时间减 7 天)
                start = LocalDateTime.now().minusWeeks(1).withHour(0).withMinute(0).withSecond(0);
            }

            LocalDateTime end;
            if (StringUtils.hasText(request.getEndTime())) {
                end = parseDateTime(request.getEndTime(), true);
            } else {
                // 默认今天结束 (23:59:59)
                end = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
            }

            predicates.add(cb.between(root.get("verifyTime"), start, end));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AttendanceLog> domainPage = logRepo.findAll(spec, pageable);
        return domainPage.map(this::convertToResponse);
    }

    private AttendanceLogDto convertToResponse(AttendanceLog log) {
        AttendanceLogDto res = new AttendanceLogDto();
        BeanUtils.copyProperties(log, res);
        if (log.getEmployee() != null) {
            res.setUserName(log.getEmployee().getName());
        } else {
            res.setUserName("未知员工");
        }
        return res;
    }

    private LocalDateTime parseDateTime(String text, boolean isEndTime) {
        if (!StringUtils.hasText(text)) return null;

        text = text.trim();
        try {
            if (text.length() == 10) {
                // 如果只有日期，根据是开始还是结束时间，补上 00:00:00 或 23:59:59
                if (isEndTime) {
                    return LocalDate.parse(text, DATE_FORMATTER).atTime(LocalTime.MAX); // 23:59:59.999...
                } else {
                    return LocalDate.parse(text, DATE_FORMATTER).atStartOfDay(); // 00:00:00
                }
            }
            // 如果长度不对，尝试按标准格式解析
            return LocalDateTime.parse(text, DATE_TIME_FORMATTER);
        } catch (Exception e) {
            // 解析失败时的兜底逻辑，或者抛出更友好的异常
            return null;
        }
    }
}
