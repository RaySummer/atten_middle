package com.ray.atten.middle.module.service;

import com.ray.atten.middle.module.model.Employee;
import com.ray.atten.middle.module.repository.EmployeeRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmployeeService {

    @Autowired
    public EmployeeRepository employeeRepository;

    public List<Employee> processBioData(String sn, String bodyData) {
        log.debug("Processing BIODATA data from: " + sn);

        List<Employee> employees = new ArrayList<>();

        // 1. 按行分割数据 (每个模板可能占据多行，但键值对通常在一行)
        String[] records = bodyData.trim().replace("BIODATA", "").split("\n");

        //如果多行多个数据需要采用这个方式
        List<Map<String, String>> dataList = new ArrayList<>();

        for (String line : records) {
            // BIODATA 的特殊性：它是键值对格式，需要先收集所有键值
            Map<String, String> currentRecord = new HashMap<>();
            if (line.isEmpty()) continue;
            log.debug("list data:" + line);
            String[] cols = line.trim().split("\t");
            for (String col : cols) {
                if (col.isEmpty()) continue;

                // 2. 按等号 (=) 分割键值对
                String[] parts = col.split("=", 2);
                if (parts.length == 2) {
                    currentRecord.put(parts[0].trim(), parts[1].trim());
                } else {
                    // 如果一行不是键值对，可能是多行模板数据，暂时不处理
                }
                log.debug("current col : " + col);
            }
            dataList.add(currentRecord);
        }

        for (Map<String, String> map : dataList) {
            // 3. 把员工信息保存到服务器的数据库
            if (!map.isEmpty()) {
                Employee data = null;
                if (StringUtils.isNoneEmpty(map.get("Pin"))) {
                    data = employeeRepository.findByPin(map.get("Pin"));
                }
                if (data == null) {
                    data = new Employee();
                }
                data.setPin(map.get("Pin"));
                data.setBiologyNo(map.get("No"));
                if (StringUtils.isNoneEmpty(data.getSyncedFromDeviceSn())) {
                    String orgSn = data.getSyncedFromDeviceSn();
                    if (!orgSn.contains(sn)) {
                        data.setSyncedFromDeviceSn(orgSn + "," + sn);
                    }
                } else {
                    data.setSyncedFromDeviceSn(sn);
                }
                data.setType(Integer.parseInt(map.get("Type")));
                data.setValid(Integer.parseInt(map.get("Valid")));
                data.setFormat(map.get("Format"));
                data.setMajorVer(map.get("MajorVer"));
                data.setMinorVer(map.get("MinorVer"));
                data.setTmp(map.get("Tmp"));

                employeeRepository.save(data);
                employees.add(data);
                log.debug("Saved BioData: " + data);
            }
        }
        return employees;
    }

}
