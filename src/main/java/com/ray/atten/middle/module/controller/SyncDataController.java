package com.ray.atten.middle.module.controller;

import com.ray.atten.middle.module.aspect.LogOperation;
import com.ray.atten.middle.module.dto.EmployeeSyncRequest;
import com.ray.atten.middle.module.dto.GlobalResponseBody;
import com.ray.atten.middle.module.model.EmployeeSyncQueue;
import com.ray.atten.middle.module.repository.EmployeeSyncQueueRepository;
import com.ray.atten.middle.module.service.SyncDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/sync")
public class SyncDataController {

    @Autowired
    private SyncDataService syncService;

    @Autowired
    private EmployeeSyncQueueRepository employeeSyncQueueRepository;

    /**
     * 接收前端上传的员工数据，并推送到所有机器
     * 请求方式: POST (multipart/form-data)
     */
    @LogOperation("同步数据到考勤机")
    @PostMapping("/sync-employee")
    public ResponseEntity<GlobalResponseBody> syncEmployee(@RequestBody List<EmployeeSyncRequest> empList) {

        if (empList.isEmpty()) {
            return ResponseEntity.ok(new GlobalResponseBody("500", "ERROR", "没有同步的数据"));
        }
        List<EmployeeSyncQueue> syncQueues = new ArrayList<>();
        for (EmployeeSyncRequest request : empList) {
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

            log.debug("Pin " + request.getPin() + " 保存成功！");
            employeeSyncQueueRepository.save(syncQueue);

            syncQueues.add(syncQueue);
        }
        syncService.syncUserToDevices(syncQueues);

        return ResponseEntity.ok(new GlobalResponseBody("200", "SUCCESS", "已保存指令到数据库，等待下次设备心跳时发送"));
    }

    @LogOperation("同步新数据")
    @PostMapping("/sync-new-data")
    public ResponseEntity<GlobalResponseBody> syncCheckNewData(@RequestBody List<String> deviceSns) {
        if (deviceSns.isEmpty()) {
            return ResponseEntity.ok(new GlobalResponseBody("500", "ERROR", "考勤机序列号不能为空！！"));
        }
        syncService.checkNewDataSendToServer(deviceSns);
        return ResponseEntity.ok(new GlobalResponseBody("200", "SUCCESS", "已保存指令到数据库，等待下次设备心跳时发送"));
    }

}
