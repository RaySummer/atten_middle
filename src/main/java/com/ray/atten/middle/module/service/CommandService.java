package com.ray.atten.middle.module.service;

import com.ray.atten.middle.module.dto.PendingCommandDto;
import com.ray.atten.middle.module.model.DeviceCommand;
import com.ray.atten.middle.module.repository.DeviceCommandRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class CommandService {

    private static final Integer STATUS_PENDING = 0;
    private static final Integer STATUS_SENT = 1;
    private static final Integer STATUS_EXECUTED = 2;
    private static final Integer STATUS_FAILED = 3; // 新增失败状态
    // 假设 ADMS 协议的指令格式是 CMD:指令内容

    @Autowired
    private DeviceCommandRepository commandRepository;

    /**
     * 供 Admin API 或其他服务调用：保存一条新的待发送指令
     *
     * @param deviceSn       目标设备序列号
     * @param commandContent 指令内容 (例如 GETUSER)
     */
    @Transactional
    public void saveNewCommand(String deviceSn, String commandContent) {
        DeviceCommand command = new DeviceCommand();
        command.setDeviceSn(deviceSn);
        System.out.println("---------------------");
        System.out.println(commandContent);
        System.out.println("---------------------");
        command.setCommandContent(commandContent);
        command.setStatus(STATUS_PENDING);
        commandRepository.save(command);
        log.debug("CommandService: New PENDING command saved for " + deviceSn + ": " + commandContent);
    }

    /**
     * 供 ZKDeviceController 握手接口调用：获取待发送指令列表
     * 【重要】: 获取指令后立即更新状态为 SENT (已发送), 防止设备重复执行
     * * @param deviceSn 正在心跳的设备序列号
     *
     * @return 格式化后的指令列表 (例如: C:GETUSER)
     */
    @Transactional
    public List<PendingCommandDto> getPendingCommands(String deviceSn) {
        // 1. 查找所有待发送的指令
        List<DeviceCommand> pendingList = commandRepository.findByDeviceSnAndStatus(deviceSn, STATUS_PENDING);

        if (pendingList.isEmpty()) {
            return null; // 返回空列表
        }
        List<PendingCommandDto> pendingCommandDtoList = new ArrayList<>();

        // 注意：ADMS 协议要求指令前缀是 C:
        // 3. 将这些指令的状态更新为 STATUS_SENT (已发送)
        for (DeviceCommand command : pendingList) {
            // 构造 ADMS 协议要求的返回格式 (CMD:C:指令内容)
            String cmd = "C:" + command.getId() + ":" + command.getCommandContent();

            commandRepository.updateCommandStatus(command.getId(), STATUS_SENT);

            PendingCommandDto dto = new PendingCommandDto();
            dto.setCommands(cmd);
            dto.setDeviceCommands(command);
            pendingCommandDtoList.add(dto);
        }

        log.debug("CommandService: Retrieved " + pendingList.size() + " commands for " + deviceSn + " and marked as SENT.");

        return pendingCommandDtoList;
    }

    /**
     * 处理设备指令执行结果回调
     *
     * @param deviceSn   报告结果的设备序列号
     * @param cmdContent 指令内容 (例如 C:GETUSER)
     * @param returnCode 设备的返回码 ("0"为成功)
     */
    @Transactional
    public void processCommandCallback(String deviceSn, String cmdContent, String returnCode, String cmdID) {

        // 1. 提取指令的实际内容 (去除 'C:' 前缀)
        String actualContent = cmdContent != null && cmdContent.startsWith("C:")
                ? cmdContent.substring(2)
                : cmdContent;

        if (StringUtils.isEmpty(actualContent)) {
            log.error("Callback Error: Missing command content from " + deviceSn);
            return;
        }

        if (StringUtils.isEmpty(cmdID)) {
            log.error("Callback Error: Missing cmdID from " + cmdID);
            return;
        }

        Optional<DeviceCommand> deviceCommand = commandRepository.findById(Long.valueOf(cmdID));


        //处理查找到的最新的指令
        DeviceCommand command = deviceCommand.get();
        Integer newStatus;

        if ("0".equals(returnCode)) {
            // Return=0 表示成功执行
            newStatus = STATUS_EXECUTED;
            command.setStatus(newStatus);
            log.debug("Command Success: " + command.getCommandContent() + " executed on " + deviceSn);
        } else {
            // 其他返回码表示失败
            newStatus = STATUS_FAILED;
            command.setStatus(newStatus);
            log.error("Command FAILED (" + returnCode + "): " + command.getCommandContent() + " on " + deviceSn);
        }

        // 4. 更新指令状态
//        commandRepository.updateCommandStatus(latestCommand.getId(), newStatus);
        commandRepository.save(command);
    }

    @Transactional
    public void processCommandCallback(String deviceSn, String backMsg) {

        if (StringUtils.isEmpty(backMsg)) {
            return;
        }
        String returnCode = "";
        String cmdContent = "";
        String cmdId = "";
        String[] params = backMsg.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();

                if (key.equalsIgnoreCase("Return")) {
                    returnCode = value;
                } else if (key.equalsIgnoreCase("CMD")) {
                    cmdContent = value;
                } else if (key.equalsIgnoreCase("ID")) {
                    cmdId = value;
                }

            }
        }
        // 1. 提取指令的实际内容 (去除 'C:' 前缀)
        String actualContent = StringUtils.isNotEmpty(cmdContent) && cmdContent.startsWith("C:")
                ? cmdContent.substring(2)
                : cmdContent;

        if (StringUtils.isEmpty(actualContent)) {
            log.error("Callback Error: Missing command content from " + deviceSn);
            return;
        }

        if (StringUtils.isEmpty(cmdId)) {
            log.error("Callback Error: Missing cmdID from " + cmdId);
            return;
        }

        Optional<DeviceCommand> deviceCommand = commandRepository.findById(Long.valueOf(cmdId));

        //处理查找到的最新的指令
        DeviceCommand command = deviceCommand.get();
        Integer newStatus;

        if ("0".equals(returnCode)) {
            // Return=0 表示成功执行
            newStatus = STATUS_EXECUTED;
            command.setStatus(newStatus);
            log.debug("Command Success: " + command.getCommandContent() + " executed on " + deviceSn);
        } else {
            // 其他返回码表示失败
            newStatus = STATUS_FAILED;
            command.setStatus(Integer.valueOf(returnCode));
            log.error("Command FAILED (" + returnCode + "): " + command.getCommandContent() + " on " + deviceSn);
        }

        // 4. 更新指令状态
        commandRepository.save(command);
    }
}
