package com.ray.atten.middle.module.service;

import com.ray.atten.middle.module.dto.PendingCommandDto;
import com.ray.atten.middle.module.model.DeviceCommand;
import com.ray.atten.middle.module.repository.DeviceCommandRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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

        // 2. 构造 ADMS 协议要求的返回格式 (CMD:C:指令内容)
        // 注意：ADMS 协议要求指令前缀是 C:
//        List<String> commandStrings = new ArrayList<>();
//        for (DeviceCommand cmd : pendingList) {
//            String s = "C:" + cmd.getId() + ":" + cmd.getCommandContent();
//            commandStrings.add(s);
//        }

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
     * 供 /cdata POST 接口调用：设备确认指令执行成功后，更新状态
     * 实际中，设备会返回一个 OpStamp 和 CMD:OK，表示成功
     *
     * @param commandId 指令ID
     */
    @Transactional
    public void markCommandAsExecuted(Long commandId) {
        commandRepository.updateCommandStatus(commandId, 2); // 2=已执行
        log.debug("CommandService: Command ID " + commandId + " marked as EXECUTED.");
    }

    /**
     * 处理设备指令执行结果回调
     *
     * @param deviceSn   报告结果的设备序列号
     * @param cmdContent 指令内容 (例如 C:GETUSER)
     * @param returnCode 设备的返回码 ("0"为成功)
     */
    @Transactional
    public void processCommandCallback(String deviceSn, String cmdContent, String returnCode) {

        // 1. 提取指令的实际内容 (去除 'C:' 前缀)
        String actualContent = cmdContent != null && cmdContent.startsWith("C:")
                ? cmdContent.substring(2)
                : cmdContent;

        if (actualContent == null || actualContent.isEmpty()) {
            log.error("Callback Error: Missing command content from " + deviceSn);
            return;
        }

        // 2. 查找最近的已发送 (SENT) 指令
        List<DeviceCommand> commands = commandRepository.findByDeviceSnAndCommandContentAndStatusOrderByCreateTimeDesc(
                deviceSn, actualContent, STATUS_SENT);

        if (commands.isEmpty()) {
            log.debug("Command not found or already processed for " + actualContent + " on " + deviceSn);
            return;
        }

        // 3. 处理查找到的最新的指令
        DeviceCommand latestCommand = commands.get(0);
        Integer newStatus;

        if ("0".equals(returnCode)) {
            // Return=0 表示成功执行
            newStatus = STATUS_EXECUTED;
            log.debug("Command Success: " + latestCommand.getCommandContent() + " executed on " + deviceSn);
        } else {
            // 其他返回码表示失败
            newStatus = STATUS_FAILED;
            log.error("Command FAILED (" + returnCode + "): " + latestCommand.getCommandContent() + " on " + deviceSn);
        }

        // 4. 更新指令状态
        commandRepository.updateCommandStatus(latestCommand.getId(), newStatus);
    }

    /**
     * 處理來自 /getrequest 心跳的隱性指令執行結果回饋
     *
     * @param deviceSn   設備序列號
     * @param cmdContent 設備回傳的上次執行的指令內容 (例如 GETUSER)
     * @param returnCode 設備回傳的狀態碼 (例如 0=成功)
     */
    @Transactional
    public void processHeartbeatCallback(String deviceSn, String cmdContent, String returnCode) {

        // ADMS 設備回傳的指令內容通常是純粹的命令名，例如 "GETUSER"
        String actualContent = cmdContent.trim();

        // 1. 查找最近的已發送 (SENT) 指令
        // 狀態仍然是 STATUS_SENT (1)，我們正在等待設備確認
        List<DeviceCommand> commands = commandRepository.findByDeviceSnAndCommandContentAndStatusOrderByCreateTimeDesc(
                deviceSn, actualContent, STATUS_SENT);

        if (commands.isEmpty()) {
            log.debug("Heartbeat Callback: No matching SENT command found for " + actualContent + " on " + deviceSn);
            return;
        }

        // 2. 處理查找到的最新的指令
        DeviceCommand latestCommand = commands.get(0);
        Integer newStatus;

        if ("0".equals(returnCode) || "OK".equalsIgnoreCase(returnCode)) {
            // 設備返回 "0" 或 "OK" 表示成功執行
            newStatus = STATUS_EXECUTED;
            log.debug("Heartbeat Success: " + latestCommand.getCommandContent() + " marked EXECUTED on " + deviceSn);
        } else {
            // 任何其他值或空值都視為失敗
            newStatus = STATUS_FAILED;
            log.error("Heartbeat FAILED (" + returnCode + "): " + latestCommand.getCommandContent() + " on " + deviceSn);
        }

        // 3. 更新指令狀態
        commandRepository.updateCommandStatus(latestCommand.getId(), newStatus);
    }
}
