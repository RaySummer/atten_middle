package com.ray.atten.middle.module.controller;

import com.ray.atten.middle.module.dto.PendingCommandDto;
import com.ray.atten.middle.module.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.List;

@Slf4j
@RestController
public class ZKDeviceController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private CommandService commandService;

    @Autowired
    private SyncDataService syncService;

    @Autowired
    private ParseDataService parseDataService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private DeviceConfigService deviceConfigService;

    /**
     * 1. 初始化/握手接口
     * 设备启动或修改配置时会访问
     */
/*    @GetMapping("/iclock/cdata")
    public String handshake(@RequestParam(value = "SN", required = false) String sn,
                            @RequestParam(value = "options", required = false) String options,
                            HttpServletRequest request) {

        // =========================================================
        // 【调试代码块】 打印所有接收到的查询参数
        // =========================================================
        log.debug("--- RAW handshake DATA START (SN: " + sn + ") ---");
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            // 获取参数的所有值 (GET请求通常只有一个值)
            String[] paramValues = request.getParameterValues(paramName);
            for (String value : paramValues) {
                log.debug(paramName + ": " + value);
            }
        }
        log.debug("--- RAW handshake DATA END ---");
        // =========================================================

        // 简单返回 OK，避免影响后续的 getrequest
        StringBuilder response = new StringBuilder();
        response.append("GET OPTION FROM: ")
                .append(sn)
                .append("\n")
                .append("USERINFOStamp=USERINFO\n")
                .append("ATTLOGStamp=ATTLOG\n")
                .append("OPERLOGStamp=None\n")
                .append("ATTPHOTOStamp=None\n")
                .append("ErrorDelay=30\n")
                .append("Delay=10\n")
                .append("TransTimes=00:05;14:05\n")
                .append("TransInterval=1\n")
                .append("TransFlag=TransData\n")
                .append("AttLog\tOpLog\tAttPhoto\tEnrollUser\tChgUser\tEnrollFP\tChgFP\tUserPic\n")
                .append("TimeZone=8\n")
                .append("Realtime=1\n")
                .append("Encrypt=0\n")
                .append("PushProtVer=2.4.1\n");

        return "OK";

//        // 假设您有一个 CommandService 来获取待发送的指令列表
//        List<String> pendingCommands = commandService.getPendingCommands(sn);
//
//        StringBuilder response = new StringBuilder();
//        response.append("GET OPTION FROM: ").append(sn).append("\n");
//
//        // --- 1. 服务器配置 ---
//        response.append("ATTLOGStamp=9999\n");
//        response.append("Delay=30\n"); // 推荐下次30秒后重试
//        // ... 其它配置 ...
//
//        // --- 2. 检查并添加指令 ---
//        if (pendingCommands != null && !pendingCommands.isEmpty()) {
//            for (String command : pendingCommands) {
//                // 命令格式必须是 CMD:C:指令名 (例如 C:GETUSER)
//                response.append("CMD:").append(command).append("\n");
//            }
//        }
//
//        // --- 3. 结束标志 ---
//        response.append("OK\n");
//
//        return response.toString();
    }*/

    /**
     * 1. 协议规范：初始化信息交互接口
     * 作用：服务器向客户端下发配置参数。
     * URL: GET /iclock/cdata?SN=xxx&options=all...
     */
    @GetMapping("/iclock/cdata")
    public String handleCdataGet(@RequestParam("SN") String sn,
                                 HttpServletRequest request) {

        // 1. 构建 HTTP 响应头
        StringBuilder httpResponse = new StringBuilder();
        httpResponse.append("HTTP/1.1 200 OK\n");

        // 必须包含 Date 头域（用于同步时间，使用 GMT 格式）
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
        String dateHeader = ZonedDateTime.now(ZoneOffset.UTC).format(formatter);
        httpResponse.append("Date: ").append(dateHeader).append("\n");

        // 关键：Content-Type 和 Connection Close (示例中使用了 close)
        httpResponse.append("Content-Type: text/plain\n");
        httpResponse.append("Connection: close\n");
        httpResponse.append("Pragma: no-cache\n");
        httpResponse.append("Cache-Control: no-store\n");

        // --- 2. 构建 Body 内容 (配置信息) ---
        StringBuilder bodyContent = new StringBuilder();

        // 协议要求：第一行必须是 GETOPTIONFROM: ${SerialNumber}
        bodyContent.append("GET OPTION FROM: ").append(sn).append("\n");

        // 协议要求：配置信息使用键值对形式 (Key=Value)，用 ${LF} (换行符) 间隔

        // 关键配置项 (根据协议要求，这里设置为最合理的默认值)
        bodyContent.append("USERINFOStamp=0\n");
        bodyContent.append("ATTLOGStamp=0\n");       // 设为 0，强制设备重新上传所有考勤记录
        bodyContent.append("OPERLOGStamp=0\n");      // 设为 0
        bodyContent.append("ErrorDelay=30\n");       // 联网失败重试间隔 (秒)
        bodyContent.append("Delay=10\n");            // 正常心跳间隔 (秒)
        bodyContent.append("TransTimes=00:00\n");    // 定时传送时间
        bodyContent.append("TransInterval=1\n");     // 传送新数据间隔 (分钟)
        bodyContent.append("TransFlag=TransData AttLog\n"); // 允许自动上传考勤记录
        bodyContent.append("TimeZone=8\n");          // 服务器时区 (东八区)
        bodyContent.append("Realtime=1\n");          // 实时传送新记录
        bodyContent.append("ServerVer=2.2.14\n");    // 协议最低版本要求
        bodyContent.append("PushProtVer=2.4.2\n");   // 告知设备服务器支持的协议版本

        // 其他可选参数可以省略，或根据需要添加

        // --- 3. 合并 HTTP 响应 ---
        // Content-Length 必须是 Body 长度
        httpResponse.append("Content-Length: ").append(bodyContent.length()).append("\n\n");

        // 附加 Body 内容
        httpResponse.append(bodyContent);

        return httpResponse.toString();
    }

    /**
     * 2. 接收考勤数据 (POST)
     * URL: /iclock/cdata?SN=xxx&table=ATTLOG
     */
   /* @PostMapping(value = {"/iclock/cdata", "/cdata"})
    public String receiveData(@RequestParam(value = "SN") String sn,
                              @RequestParam(value = "table") String table,
                              @RequestParam(value = "Stamp") String stamp,
                              @RequestBody String data,
                              HttpServletRequest request) throws IOException {

        // =========================================================
        // 【调试代码块】 打印所有接收到的查询参数
        // =========================================================
        log.debug("--- RAW receive DATA START (SN: " + sn + ") ---");
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            // 获取参数的所有值 (GET请求通常只有一个值)
            String[] paramValues = request.getParameterValues(paramName);
            for (String value : paramValues) {
                log.debug(paramName + ": " + value);
            }
        }
        log.debug("--- RAW receive DATA END ---");
        // =========================================================


        if (data.contains("CData: AttLog")) {
            List<String> attLogList = parseDataService.parseAttLogDataFromCData(data);
            if (!attLogList.isEmpty()) {
                attendanceService.processAndSaveAttendance(sn, attLogList);
            }

            log.debug("收到设备[" + sn + "] 考勤记录: " + attLogList.size() + "条");
        }
        if (data.contains("CData: User")) {
            // 假设您已经将原始数据解析成 List<String> userList
            // 示例: ["10001\t张三\t123456\t1", "10002\t李四\t654321\t0"]

            List<String> userList = parseDataService.parseUserDataFromCData(data); // 这是一个假设的解析方法

            if (!userList.isEmpty()) {
                syncService.processAndSaveUsers(sn, userList);
            }

            log.debug("收到设备[" + sn + "] 考勤记录: " + userList.size() + "条");
        }
        return "OK";
    }*/
    @PostMapping(value = {"/iclock/cdata", "/cdata", "/data"}, consumes = "*/*")
    public String handleCDataPost(
            @RequestParam(value = "SN", required = false) String snParam,
            @RequestParam(value = "table", required = false) String table, // 接收 table 参数
            @RequestParam(value = "Stamp", required = false) String stamp,
            HttpServletRequest request) {

        // 1. 获取 SN 和 table 信息
        String sn = snParam;
        log.debug("\n>>> CONTROLLER: 捕获到 POST 请求！SN=" + sn + ", Table=" + table);

        // 2. 手动读取 Body 字符串 (这是最关键的步骤)
        String bodyData = "";
        String primaryEncoding = "GBK"; // 中控设备中文编码标准
        String fallbackEncoding = "UTF-8"; // 兼容英文/Base64数据的备用编码

        // 1. 确保使用 request.getInputStream() 来读取原始字节
        try (java.io.InputStream inputStream = request.getInputStream()) {

            int contentLength = request.getContentLength();
            if (contentLength <= 0) {
                log.error("!!! CONTROLLER WARNING: Content-Length is zero or missing. !!!");
                return "OK";
            }

            // 2. 将数据读取到字节数组中
            byte[] buffer = new byte[contentLength];
            int read;
            int totalRead = 0;

            // 循环读取，确保所有字节都被读完
            while (totalRead < contentLength && (read = inputStream.read(buffer, totalRead, contentLength - totalRead)) != -1) {
                totalRead += read;
            }

            if (totalRead > 0) {
                // 3. 核心步骤：使用 GBK 编码将字节转换为字符串
                bodyData = new String(buffer, 0, totalRead, fallbackEncoding);

                log.debug(">>> CONTROLLER: 原始 Body 内容长度: " + bodyData.length());
                log.debug(">>> CONTROLLER: 原始 Body 内容 (使用 " + fallbackEncoding + " 解码):\n" + bodyData.substring(0, Math.min(bodyData.length(), 1000)));
                if (bodyData.length() > 1000) {
                    log.debug("...(Body Truncated)...");
                }

                if ("USERINFO".equalsIgnoreCase(table)) {
                    // 您的用户数据解析逻辑
                    // deviceSyncService.processUserInfo(sn, bodyData);
                    log.debug("--- 成功捕获到 USERINFO 数据！---");
                } else if ("ATTLOG".equalsIgnoreCase(table)) {
                    try {
                        attendanceService.processAttLogData(sn, bodyData);
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error("考勤记录接收成功，但数据保存异常。");
                    }

                    log.debug("--- 收到设备[" + sn + "] 考勤记录---");
                } else if ("BIODATA".equalsIgnoreCase(table)) {
                    log.debug("--- 进入了BIODATA的判断，准备执行保存 ---");
                    try {
                        employeeService.processBioData(sn, bodyData);
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error("BIODATA接收成功，但数据保存异常。");
                    }
                    log.debug("--- 成功捕获到 BIODATA 数据！---");
                } else {
                    // 其他数据类型

                    log.debug("--- 成功捕获到 其他类型 数据！---");
                }

            } else {
                log.error("!!! CONTROLLER ERROR: Body is empty after reading !!!");
            }

        } catch (Exception e) {
            // 如果这里仍然捕获到异常，则可能是读取过程中断
            log.error("Critical Error reading POST body: " + e.getMessage());
            e.printStackTrace();
        }

        // 协议要求：必须返回 OK，否则设备会重传数据
        return "OK";
    }

    /**
     * 3. 设备心跳 & 拉取指令
     * URL: /iclock/getrequest?SN=xxx
     */
    @GetMapping("/iclock/getrequest")
    public String heartbeat(@RequestParam("SN") String sn) {

        // 注意：/getrequest 的响应体只需要配置行和 CMD 指令，不需要 HTTP 头部
        StringBuilder response = new StringBuilder();
        response.append("GET OPTION FROM: ").append(sn).append("\n");

        response.append("USERINFOStamp=9999\n");
        response.append("ATTLOGStamp=9999\n");       // 设为 0，强制设备重新上传所有考勤记录
//        response.append("OPERLOGStamp=0\n");      // 设为 0
        response.append("ErrorDelay=30\n");       // 联网失败重试间隔 (秒)

        response.append("Delay=10\n"); // 确保这里也设置了 Delay
        // ... (指令下发逻辑) ...
        List<PendingCommandDto> pendingCommands = commandService.getPendingCommands(sn);

        // 3. 附加指令
        if (pendingCommands != null && !pendingCommands.isEmpty()) {
//            deviceConfigService.getLastOneLogByOperation()
            for (PendingCommandDto command : pendingCommands) {
//                 C:${CmdID}:DATA${SP}QUERY${SP}USERINFO${SP}PIN=${XXX}
//                转换成您代码中需要拼接的字符串，就是：
//                C:12345:DATA QUERY USERINFO PIN=1001
                response.append(command.getDeviceCommands()).append("\n");
            }
        }

        // 4. 结束标记 (必须)
        response.append("OK");

        log.debug("Heartbeat received from " + sn + ". Sending " + (pendingCommands == null ? 0 : pendingCommands.size()) + " command(s).");

        log.debug("Heartbeat data : " + response);

        return response.toString();
    }

    /**
     * 4. 指令执行结果回调
     * 设备执行完服务器下发的指令后，将结果通过此接口告知服务器。
     * URL: /iclock/device-cmd?SN=xxx&Return=0&CMD=C:GETUSER
     */
    @PostMapping("/iclock/devicecmd")
    public String commandCallback(
            @RequestParam("SN") String sn,
            HttpServletRequest request) {

        // =========================================================
        // 【调试代码块】 打印所有接收到的查询参数
        // =========================================================
        log.debug("--- RAW commandCallback DATA START (SN: " + sn + ") ---");
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            // 获取参数的所有值 (GET请求通常只有一个值)
            String[] paramValues = request.getParameterValues(paramName);
            for (String value : paramValues) {
                log.debug(paramName + ": " + value);
            }
        }
        log.debug("--- RAW commandCallback DATA END ---");
        // =========================================================

        // 我们知道 Body 里包含指令执行结果，例如 "Return=0\tCMD=C:GETUSER"
        String bodyData = "";
        try {
            // 手动从 request 中读取 Body (Content-Type: application/octet-stream)
            bodyData = request.getReader().lines().collect(java.util.stream.Collectors.joining("\n"));
            log.debug("DEBUG: DeviceCMD POST Body received: " + bodyData);

        } catch (java.io.IOException e) {
            log.error("Error reading devicecmd body: " + e.getMessage());
        }

        // 1. 尝试从 Query 或 Body 中提取 Return 和 CMD
        String returnCode = request.getParameter("Return");
        String cmdContent = request.getParameter("CMD");

        if (!bodyData.isEmpty()) {
            // 使用 & 符号分割参数
            String[] params = bodyData.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim();

                    if (key.equalsIgnoreCase("Return")) {
                        returnCode = value;
                    } else if (key.equalsIgnoreCase("CMD")) {
                        cmdContent = value;
                    }
                    // 也可以打印 ID，方便追踪
                    if (key.equalsIgnoreCase("ID")) {
                        log.debug("Callback CMD ID: " + value);
                    }
                }
            }
        }

        // 2. 调用 CommandService 处理结果 (继续使用旧逻辑)
        if (returnCode != null) { // 只需要 Return，因为 CMD 可能是空的
            // 如果 Return=-1002，CommandService 会将指令标记为 FAILED
            commandService.processCommandCallback(sn, cmdContent, returnCode);
        } else {
            log.error("DeviceCMD callback missing Return info.");
        }

        // 必须返回 OK，告知设备服务器已收到结果。
        return "OK";
    }

    /**
     * 连接检查接口 (关键！)
     * 设备在发送大数据之前，会调用此接口测试连通性
     */
    @GetMapping("/ping")
    public String ping() {
        // 必须返回 OK
        return "OK";
    }

    /**
     * 6. 设备注册接口 (部分设备启动时调用)
     * 用于提交设备型号、序列号、固件版本等信息
     */
    @PostMapping("/registry")
    public String registry(@RequestParam("SN") String sn, @RequestBody(required = false) String data) {
        log.debug("Device Registry: " + sn);
        // 这里可以更新 Device 表中的 info/固件版本等信息
        // 必须返回 OK，否则设备会一直尝试注册
        return "registry=ok";
    }


}
