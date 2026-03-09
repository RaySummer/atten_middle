package com.ray.atten.middle.module.controller;

import com.ray.atten.middle.module.dto.*;
import com.ray.atten.middle.module.service.AttendanceGroupService;
import com.ray.atten.middle.module.service.AttendanceService;
import com.ray.atten.middle.module.service.DeviceService;
import com.ray.atten.middle.module.service.OaEmployeeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OaEmployeeController {

    @Autowired
    private OaEmployeeService oaEmployeeService;

    @Autowired
    private AttendanceGroupService attendanceGroupService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private DeviceService deviceService;


    // todo: 增加显示是否录入指纹人脸，增加分部显示，排序优先显示未录入按时间倒序

    /**
     * 使用 POST 請求查詢員工列表 (支持複雜查詢和分頁參數)
     * * @param request 包含所有查詢條件的 DTO
     *
     * @return 員工列表的 Page 對象 (包含內容、總頁數、總記錄數等)
     */
    @PostMapping("/oa-employees/query")
    public ResponseEntity<GlobalResponseBody> queryEmployees(@RequestBody OaEmployeeQueryPageRequest request) {
        return ResponseEntity.ok(new GlobalResponseBody("200", "SUCCESS", oaEmployeeService.queryEmployees(request)));
    }

    /**
     * 接收第三方系統推送的員工數據。
     * 接口快速返回，數據處理異步進行。
     */
    @PostMapping("/oa-employees/push")
    public ResponseEntity<GlobalResponseBody> receiveEmployeePush(@RequestBody List<OaEmployeeRequest> employees) {
        if (employees == null || employees.isEmpty()) {
            return ResponseEntity.ok(new GlobalResponseBody("500", "ERROR", "推送数据队列为空"));
        }

        // 立即調用異步 Service 方法，Controller 線程會立即釋放
        oaEmployeeService.asyncBatchUpsert(employees);

        // 快速返回響應，不等待數據庫操作完成
        return ResponseEntity.ok(new GlobalResponseBody("200", "SUCCESS", "推送数据成功，一共" + employees.size() + "条数据"));
    }

    /**
     * 获取所有设备组数据
     */
    @GetMapping("/atten/group")
    public ResponseEntity<GlobalResponseBody> queryAttenGroup() {
        return ResponseEntity.ok(new GlobalResponseBody("200", "SUCCESS", attendanceGroupService.queryAll()));
    }

    /**
     * 接收第三方推送的设备组数据
     *
     * @return
     */
    @PostMapping("/atten/group")
    public ResponseEntity<GlobalResponseBody> pushAttenGroup(@RequestBody AttendanceGroupRequest attendanceGroupRequest) {

        if (StringUtils.isEmpty(attendanceGroupRequest.getGroupName()) || attendanceGroupRequest.getDeviceSns().isEmpty()) {
            return ResponseEntity.ok(new GlobalResponseBody("500", "ERROR", "推送数据为空"));
        }
        try {
            attendanceGroupService.saveOrUpdateGroup(attendanceGroupRequest);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new GlobalResponseBody("500", "ERROR", e.getMessage()));
        }
        return ResponseEntity.ok(new GlobalResponseBody("200", "SUCCESS", "设备组推送成功"));
    }

    /**
     * 分頁查詢考勤日誌
     * 使用 POST 或 GET 均可，若使用 GET 且 RequestBody 則需注意某些客戶端限制，
     * 這裡建議使用 POST，或者將參數平鋪。若要支持 Desktop，POST 通常更方便處理復雜 DTO。
     */
    @PostMapping("/atten/query")
    public ResponseEntity<GlobalResponseBody> queryLogs(@RequestBody AttendanceLogRequest request) {
        return ResponseEntity.ok(new GlobalResponseBody("200", "SUCCESS", attendanceService.findLogPage(request)));
    }

    @PostMapping("/device/query")
    public ResponseEntity<GlobalResponseBody> queryDevice(@RequestBody DeviceRequest request) {
        return ResponseEntity.ok(new GlobalResponseBody("200", "SUCCESS", deviceService.getDeviceList(request)));
    }
}
