package com.ray.atten.middle.module.controller;

import com.ray.atten.middle.module.dto.DeviceCommandRequest;
import com.ray.atten.middle.module.dto.GlobalResponseBody;
import com.ray.atten.middle.module.service.CommandService;
import com.ray.atten.middle.module.service.SyncDataService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
public class AdminController {

    @Autowired
    private CommandService commandService;

    @PostMapping("/device-commend")
    public ResponseEntity<GlobalResponseBody> saveDeviceCommend(@RequestBody DeviceCommandRequest commandRequest) {
        if (commandRequest.getDeviceSns().isEmpty()) {
            return ResponseEntity.ok(new GlobalResponseBody("500", "错误", "设备码不能为空"));
        }
        for (String sn : commandRequest.getDeviceSns()) {
            StringBuffer sb = new StringBuffer();
            sb.append(commandRequest.getCmd());
            if (StringUtils.isNoneEmpty(commandRequest.getRecode())) {
                sb.append(" ");
                sb.append(commandRequest.getRecode());
            }
            if (StringUtils.isNoneEmpty(commandRequest.getTable())) {
                sb.append(" ");
                sb.append(commandRequest.getTable());
            }
            if ("USERINFO".equalsIgnoreCase(commandRequest.getTable())) {
                if (StringUtils.isNoneEmpty(commandRequest.getPin())) {
                    sb.append(" PIN=");
                    sb.append(commandRequest.getPin());
                }
            }
            if ("FINGERTMP".equalsIgnoreCase(commandRequest.getTable())) {
                if (StringUtils.isNoneEmpty(commandRequest.getPin())) {
                    sb.append(" PIN=");
                    sb.append(commandRequest.getPin());
                    sb.append("\t");
                }
                if (StringUtils.isNoneEmpty(commandRequest.getFID())) {
                    sb.append("FID=");
                    sb.append(commandRequest.getFID());
                }
            }
            if (commandRequest.getStartTime() != null && commandRequest.getEndTime() != null) {
                sb.append(" StartTime=");
                sb.append(commandRequest.getStartTime());
                sb.append("\tEndTime=");
                sb.append(commandRequest.getEndTime());
            }

            commandService.saveNewCommand(sn, sb.toString());
        }
        return ResponseEntity.ok(new GlobalResponseBody("200", "成功", "已保存指令到数据库，等待下次设备心跳时发送"));
    }

}
