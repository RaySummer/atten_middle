package com.ray.atten.middle.module.controller;

import com.ray.atten.middle.module.dto.GlobalResponseBody;
import com.ray.atten.middle.module.dto.OaEmployeeQueryRequest;
import com.ray.atten.middle.module.model.OaEmployee;
import com.ray.atten.middle.module.service.OaEmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/oa-employees")
public class OaEmployeeController {

    @Autowired
    private OaEmployeeService oaEmployeeService;

    /**
     * 使用 POST 請求查詢員工列表 (支持複雜查詢和分頁參數)
     * * @param request 包含所有查詢條件的 DTO
     *
     * @return 員工列表的 Page 對象 (包含內容、總頁數、總記錄數等)
     */
    @PostMapping("/query")
    public ResponseEntity<GlobalResponseBody> queryEmployees(@RequestBody OaEmployeeQueryRequest request) {
        return ResponseEntity.ok(new GlobalResponseBody("200", "SUCCESS", oaEmployeeService.queryEmployees(request)));
    }

    /**
     * 接收第三方系統推送的員工數據。
     * 接口快速返回，數據處理異步進行。
     */
    @PostMapping("/push")
    public ResponseEntity<GlobalResponseBody> receiveEmployeePush(@RequestBody List<OaEmployee> employees) {
        if (employees == null || employees.isEmpty()) {
            return ResponseEntity.ok(new GlobalResponseBody("200", "SUCCESS", "推送数据队列为空"));
        }

        // 立即調用異步 Service 方法，Controller 線程會立即釋放
        oaEmployeeService.asyncBatchUpsert(employees);

        // 快速返回響應，不等待數據庫操作完成
        return ResponseEntity.ok(new GlobalResponseBody("200", "SUCCESS", "推送数据成功，一共" + employees.size() + "条数据"));
    }

}
