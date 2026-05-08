package com.ray.atten.middle.module.service;

import com.ray.atten.middle.module.model.Device;
import com.ray.atten.middle.module.model.Employee;
import com.ray.atten.middle.module.model.EmployeeSyncQueue;
import com.ray.atten.middle.module.repository.DeviceRepository;
import com.ray.atten.middle.module.repository.EmployeeRepository;
import com.ray.atten.middle.module.repository.EmployeeSyncQueueRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SyncDataService {

    @Autowired
    private DeviceRepository deviceRepo;

    @Autowired
    private CommandService commandService;

    @Autowired
    private EmployeeSyncQueueRepository queueRepo;

    @Autowired
    private EmployeeRepository employeeRepository;

    /**
     * 执行批量同步逻辑
     *
     * @return 同步了多少条员工数据
     */
    @Transactional
    public int processPendingSyncs() {
        // 1. 获取所有待处理的记录
        List<EmployeeSyncQueue> pendingList = queueRepo.findByStatus(0);

        if (pendingList.isEmpty()) {
            return 0;
        }

        // 2. 获取所有已启用的设备
        List<Device> allDevices = deviceRepo.findAll(); // 建议加 .filter(Device::isActive)

        // 3. 遍历待同步列表
        for (EmployeeSyncQueue emp : pendingList) {

            // 确定目标设备：如果是指定SN，就只发给那一台；否则发给所有设备
            List<Device> targets = new ArrayList<>();
            if (emp.getTargetDeviceSn() != null && !emp.getTargetDeviceSn().isEmpty()) {
                // 查找指定设备 (这里简单处理，实际可用Map优化性能)
                allDevices.stream()
                        .filter(d -> d.getDeviceSn().equals(emp.getTargetDeviceSn()))
                        .findFirst()
                        .ifPresent(targets::add);
            } else {
                targets = allDevices; // 广播给所有设备
            }

            // 4. 为每一台目标设备生成指令
            for (Device device : targets) {
                generateCommandsForDevice(device.getDeviceSn(), emp);
            }

            // 5. 标记该条记录为“已处理”
            emp.setStatus(1);
            queueRepo.save(emp);
        }

        return pendingList.size();
    }

    private void generateCommandsForDevice(String sn, EmployeeSyncQueue emp) {
        // --- 1. 用户基本信息 (必须最先发送) ---
        // 格式: DATA USER PIN=xxx Name=xxx Pri=0 Passwd= Card= Grp=1
        String cmdUser = String.format("DATA USER PIN=%s\tName=%s\tPri=0\tPasswd=\tCard=\tGrp=1",
                emp.getPin(), emp.getName());
        saveCommand(sn, cmdUser);

        // --- 2. 指纹信息 (如果有) ---
        if (!emp.getType().equalsIgnoreCase("photo") && StringUtils.isNotEmpty(emp.getBase64Data())) {
            // 假设是第0枚指纹
            String cmdFp = String.format("DATA FP PIN=%s\tFID=%s\tSize=%d\tValid=1\tTmp=%s",
                    emp.getPin(), emp.getFid() + "", emp.getBase64Data().length(), emp.getBase64Data());
            saveCommand(sn, cmdFp);
        }

        // --- 3. 照片信息 (如果有) ---
        if (emp.getType().equalsIgnoreCase("photo") && StringUtils.isNotEmpty(emp.getBase64Data())) {
            String rawBase64 = emp.getBase64Data();

            // 关键逻辑：去除可能存在的 Base64 前缀
            if (rawBase64.contains(",")) {
                rawBase64 = rawBase64.split(",")[1];
            }

            // 这里的 Size 是字符串长度，不是字节长度，ADMS 协议通常接受字符串长度或内容大小
            // 安全起见，XFace通常直接校验内容，Size参数可填内容长度
            String cmdPic = String.format("DATA USERPIC PIN=%s\tName=%s.jpg\tSize=%d\tContent=%s",
                    emp.getPin(), emp.getPin(), rawBase64.length(), rawBase64);
            saveCommand(sn, cmdPic);
        }
    }

    /**
     * 全网广播：同步员工数据（基本信息 + 指纹 + 照片）
     * * @param pin 员工工号
     *
     * @param name                姓名
     * @param fingerprintTemplate 指纹模板字符串 (Base64格式, 采集仪获取)
     * @param photoBytes          照片文件的字节数组
     */
    @Transactional
    public void syncUserToAllDevices(String pin, String name, String fingerprintTemplate, byte[] photoBytes) {
        // 1. 获取所有活跃的考勤机
        List<Device> devices = deviceRepo.findAll(); // 实际生产中可以加 filter(d -> d.isActive())

        for (Device device : devices) {
            String sn = device.getDeviceSn();

            // --- 指令1: 下发基本用户 ---
            // 格式: DATA USER PIN=xxx Name=xxx ...
            String cmdUser = String.format("DATA USER PIN=%s\tName=%s\tPri=0\tPasswd=\tCard=\tGrp=1", pin, name);
            saveCommand(sn, cmdUser);

            // --- 指令2: 下发指纹 (如果有) ---
            if (fingerprintTemplate != null && !fingerprintTemplate.isEmpty()) {
                // 格式: DATA FP PIN=xxx FID=索引(0-9) Size=长度 Valid=1 Tmp=模板内容
                // 注意：ZK指纹算法通常为 VX10.0，FID通常从0开始
                String cmdFp = String.format("DATA FP PIN=%s\tFID=0\tSize=%d\tValid=1\tTmp=%s",
                        pin, fingerprintTemplate.length(), fingerprintTemplate);
                saveCommand(sn, cmdFp);
            }

            // --- 指令3: 下发照片 (如果有) ---
            if (photoBytes != null && photoBytes.length > 0) {
                // 将图片字节转为 Base64
                String photoBase64 = Base64.getEncoder().encodeToString(photoBytes);

                // 格式: DATA USERPIC PIN=xxx Name=xxx.jpg Size=字节长度 Content=Base64内容
                String cmdPic = String.format("DATA USERPIC PIN=%s\tName=%s.jpg\tSize=%d\tContent=%s",
                        pin, pin, photoBytes.length, photoBase64);
                saveCommand(sn, cmdPic);
            }
        }
    }

    /**
     * 第一步：生成查询考勤机员工数据的指令 (用于 Admin API)
     *
     * @param deviceSn 考勤机序列号
     */
    public void sendQueryUserCommand(String deviceSn) {
        // ADMS 协议中，查询所有用户数据的指令是 GETUSER
        String command = "GETUSER";

        // 假设您的 CommandRepository 可以保存指令
        // 这里只是一个简化示例，实际生产环境需要更复杂的Command实体

        // 调用 CommandService 将指令保存到数据库中，状态为 PENDING (0)
        commandService.saveNewCommand(deviceSn, command);

        log.debug("DEBUG: 已为设备 " + deviceSn + " 生成指令: " + command);

        // 实际应用中，您需要将这个 command 保存到 Command 队列/表中，等待设备下次心跳时获取
    }

    /**
     * 第二步：处理设备返回的员工数据 (用于 /iclock/cdata 接口)
     * 考勤机通过 CData 接口返回数据时会调用此方法
     *
     * @param deviceSn     返回数据的设备序列号
     * @param userDataList 考勤机返回的员工数据列表
     */
    public void processAndSaveUsers(String deviceSn, List<String> userDataList) {
        if (userDataList == null || userDataList.isEmpty()) {
            return;
        }

        // 批量保存/更新的列表
        List<Employee> employeesToSave = userDataList.stream().map(data -> {
            // 假设考勤机返回的数据格式为: PIN\tName\tCard\tPrivilege
            String[] parts = data.split("\t");

            if (parts.length < 4) return null; // 数据格式不完整

            Employee employee = new Employee();
            employee.setPin(parts[0]);
            employee.setName(parts[1]);
            employee.setBiologyNo(parts[2]);

            try {
                employee.setPrivilege(Integer.parseInt(parts[3]));
            } catch (NumberFormatException e) {
                employee.setPrivilege(0);
            }

            employee.setSyncedFromDeviceSn(deviceSn);
            employee.setSyncTime(LocalDateTime.now());

            return employee;
        }).filter(e -> e != null).collect(Collectors.toList());

        // 批量保存或更新员工数据
        // 在实际项目中，您可能需要 EmployeeRepository.saveAllAndFlush(employeesToSave)
        // 并且需要处理 PIN 冲突（唯一约束）
        employeeRepository.saveAll(employeesToSave);

        log.debug("DEBUG: 已从设备 " + deviceSn + " 成功保存/更新 " + employeesToSave.size() + " 条员工数据。");
    }

    private void saveCommand(String sn, String content) {
        commandService.saveNewCommand(sn, content);
    }

    @Transactional
    public void syncUserToDevices(List<EmployeeSyncQueue> syncQueues) {

        if (syncQueues == null || syncQueues.isEmpty()) {
            log.debug("待同步的数据为空，不生成指令");
            return;
        }

        for (EmployeeSyncQueue queue : syncQueues) {
            if (queue.getStatus() == 0) {
                String[] deviceSns = queue.getTargetDeviceSn().split(",");
                //修改：如果没有同步序列号即没有选择考勤组，不生成指令
                if (deviceSns.length <= 0) {
                    log.debug("没有序列号，无法生成命令");
                    continue;
                }
                for (String sn : deviceSns) {
                    //生成更新用户指令
                    commandService.saveNewCommand(sn, generateUserInfoCMD(queue));

                    if (StringUtils.isNotEmpty(queue.getType())) {
                        if (!queue.getType().equalsIgnoreCase("photo") && StringUtils.isNotEmpty(queue.getBase64Data())) {
                            //生成更新用户指纹指令
                            System.out.println(queue.getFid());
                            commandService.saveNewCommand(sn, generateUserFinger(queue));
                            //生成登记指纹指令
                            commandService.saveNewCommand(sn, generateFingerCMD(queue));

                        }

                        if (queue.getType().equalsIgnoreCase("photo") && StringUtils.isNotEmpty(queue.getBase64Data())) {
                            //生成更新用户照片模板指令
                            commandService.saveNewCommand(sn, generateUserBIOPHOTO(queue));
                            //生成BIO指令，上传可见光图片
                            commandService.saveNewCommand(sn, generateBIOCMD(queue, "face"));
                            //生成登记人脸指令
                            commandService.saveNewCommand(sn, generatePhotoCMD(queue));
                        }
                    }
                }
                queue.setStatus(1);
                queueRepo.save(queue);
            }
        }
    }

    public void checkNewDataSendToServer(List<String> deviceSns) {
        if (deviceSns.isEmpty()) {
            log.debug("没有序列号，无法生成命令");
            return;
        }
        for (String sn : deviceSns) {
            commandService.saveNewCommand(sn, "LOG");
        }
    }

    /**
     * 拼接更新用户信息指令
     *
     * @param queue
     * @return
     */
    private String generateUserInfoCMD(EmployeeSyncQueue queue) {
        StringBuffer sb = new StringBuffer();
        sb.append("DATA ");
        sb.append("UPDATE ");
        sb.append("USERINFO ");
        sb.append("PIN=");
        sb.append(queue.getPin());
        sb.append("\t");
        sb.append("NAME=");
        sb.append(queue.getName());
        sb.append("\t");
        sb.append("Pri=");
        sb.append(queue.getPri());
        sb.append("\t");
        sb.append("Passwd=");
        sb.append(queue.getPasswd());
        sb.append("\t");
        sb.append("Card=");
        sb.append(queue.getCardNo());
        sb.append("\t");
        sb.append("Grp=0");
        sb.append("\t");
        sb.append("Verify=");
        sb.append(queue.getVerify());
        return sb.toString();
    }

    /**
     * 拼接BIOData 指令
     *
     * @param queue
     * @return
     */
    private String generateBIOCMD(EmployeeSyncQueue queue, String type) {
        StringBuffer sb = new StringBuffer();
        sb.append("DATA ");
        sb.append("UPDATE ");
        sb.append("BIODATA ");
        sb.append("PIN=");
        sb.append(queue.getPin());
        sb.append("\t");
        if (type.equals("face")) {
            sb.append("No=0\t");
        } else {
            sb.append("No=");
            sb.append(queue.getFid());
            sb.append("\t");
        }
        sb.append("Valid=1\t");
        sb.append("Duress=0\t");
        sb.append("NAME=");
        sb.append(queue.getName());
        sb.append("\t");
        if (type.equals("face")) {
            sb.append("Type=9\t");
        } else {
            sb.append("Type=0\t");
        }
        if (type.equals("face")) {
            sb.append("MajorVer=58\t");
        } else {
            sb.append("MajorVer=12\t");
        }
        if (type.equals("face")) {
            sb.append("MinorVer=38\t");
        } else {
            sb.append("MinorVer=10.3\t");
        }
        sb.append("Format=0\t");
        sb.append("Tmp=");
        sb.append(queue.getBase64Data());
        return sb.toString();
    }

    /**
     * 拼接登記指紋 指令
     *
     * @param queue
     * @return
     */
    private String generateFingerCMD(EmployeeSyncQueue queue) {
        StringBuffer sb = new StringBuffer();
        sb.append("ENROLL_FP ");
        sb.append("PIN=");
        sb.append(queue.getPin());
        sb.append("\t");
        sb.append("FID=");
        sb.append(queue.getFid());
        sb.append("\t");
        sb.append("RETRY=0");
        sb.append(queue.getRetry());
        sb.append("\t");
        sb.append("OVERWRITE=");
        sb.append(queue.getOverwrite());
        return sb.toString();
    }

    private String generatePhotoCMD(EmployeeSyncQueue queue) {
        StringBuffer sb = new StringBuffer();
        sb.append("ENROLL_BIO ");
        sb.append("TYPE=9");
        sb.append("\t");
        sb.append("PIN=");
        sb.append(queue.getPin());
        sb.append("\t");
        sb.append("FID=0");
        sb.append("\t");
        sb.append("RETRY=0");
        sb.append("\t");
        sb.append("OVERWRITE=1");
        return sb.toString();
    }

    /**
     * 拼接更新指纹模板
     *
     * @param queue
     * @return
     */
    private String generateUserFinger(EmployeeSyncQueue queue) {
        StringBuffer sb = new StringBuffer();
        sb.append("DATA ");
        sb.append("UPDATE ");
        sb.append("FINGERTMP ");
        sb.append("PIN=");
        sb.append(queue.getPin());
        sb.append("\t");
        sb.append("FID=");
        sb.append(queue.getFid());
        sb.append("\t");
        sb.append("Size=");
        sb.append(queue.getBase64Size());
        sb.append("\t");
        sb.append("Valid=");
        sb.append(queue.getValid());
        sb.append("\t");
        sb.append("TMP=");
        sb.append(queue.getBase64Data());

        System.out.println("FINGERTMP ID");
        System.out.println(sb.toString());
        return sb.toString();
    }

    /**
     * 拼接更新用户照片模板
     *
     * @param queue
     * @return
     */
    private String generateUserPhoto(EmployeeSyncQueue queue) {
        StringBuffer sb = new StringBuffer();
        sb.append("DATA ");
        sb.append("UPDATE ");
        sb.append("FACE ");
        sb.append("PIN=");
        sb.append(queue.getPin());
        sb.append("\t");
        sb.append("FID=0");
        sb.append("\t");
        sb.append("Valid=");
        sb.append(queue.getValid());
        sb.append("\t");
        sb.append("Size=");
        sb.append(queue.getBase64Size());
        sb.append("\t");
        sb.append("TMP=");
        sb.append(queue.getBase64Data());


        return sb.toString();
    }

    private String generateUserBIOPHOTO(EmployeeSyncQueue queue) {
        StringBuffer sb = new StringBuffer();
        sb.append("DATA ");
        sb.append("UPDATE ");
        sb.append("BIOPHOTO ");
        sb.append("PIN=");
        sb.append(queue.getPin());
        sb.append("\t");
        sb.append("Type=9");
        sb.append("\t");
        sb.append("Size=");
        sb.append(queue.getBase64Size());
        sb.append("\t");
        sb.append("Content=");
        sb.append(queue.getBase64Data());
        sb.append("\t");
        sb.append("Format=0");
        sb.append("\t");
        sb.append("Url=null");
        sb.append("\t");
        sb.append("PostBackTmpFlag=0");

        return sb.toString();
    }

}
