package com.ray.atten.middle.module.controller;

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
//    @PostMapping("/employee")
//    public String syncEmployee(
//            @RequestParam("pin") String pin,
//            @RequestParam("name") String name,
//            @RequestParam(value = "fingerprint", required = false) String fingerprint,
//            @RequestParam(value = "photo", required = false) MultipartFile photo) {
//
//        try {
//            byte[] photoBytes = null;
//            if (photo != null && !photo.isEmpty()) {
//                photoBytes = photo.getBytes();
//                // 建议检查图片大小，ADMS 传输大图片会很慢，最好控制在 30KB 以内
//                if (photoBytes.length > 50 * 1024) {
//                    return "Error: 照片太大，请压缩至50KB以内";
//                }
//            }
//
//            syncService.syncUserToAllDevices(pin, name, fingerprint, photoBytes);
//            return "同步指令已生成，正在等待所有考勤机拉取...";
//
//        } catch (IOException e) {
//            e.printStackTrace();
//            return "Error: " + e.getMessage();
//        }
//    }
    @PostMapping("/sync-employee")
    public ResponseEntity<GlobalResponseBody> syncEmployee(@RequestBody List<EmployeeSyncRequest> empList) {

        if (empList.isEmpty()) {
            return ResponseEntity.ok(new GlobalResponseBody("500", "error", "没有同步的数据"));
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

        return ResponseEntity.ok(new GlobalResponseBody("200", "success", "保存成功，等待发送同步指令"));
    }

}
