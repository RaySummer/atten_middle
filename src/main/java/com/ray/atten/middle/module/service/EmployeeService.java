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
                log.debug(" 进入了保存的判断了 ");
                Employee data = null;
                if (StringUtils.isNoneEmpty(map.get("Pin"))) {
                    data = employeeRepository.findByPin(map.get("Pin"));
                }
                if (data == null) {
                    data = new Employee();
                }
                data.setPin(map.get("Pin"));
                data.setBiologyNo(map.get("No"));
                data.setSyncedFromDeviceSn(sn);
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

    /*public static void main(String[] args) {

        String bodyData = "BIODATA Pin=103495\tNo=6\tIndex=0\tValid=1\tDuress=0\tType=1\tMajorVer=10\tMinorVer=0\tFormat=ZK\tTmp=TDNTUzIxAAAFcHcECAUHCc7QAAApcZEBAAAAhZ02vHC0AGAPkwAZAEt/nQCsAFkPXAD9cEQP7wDrAFkPxXAhARYP1wC9AAt97wCBAI4OPwCHcBcO7wAjAV8Pt3BBAZQO1gCEAY5/gABFAdoFrQBfcGEPEAFIAUMM23AwAA0O7QCqAXt/8QAlAIkNAgDtcBUG2AC8AL4Pw3DyAD0GzABeAH9/pgCNAGkPuwAUcUgP0wAiAWAPUnDXAFUPvwD3AaB/fAB0AOUPYgBccG8NAwExAVUPQ3AXAWMCAAGJAIp/xQAyAH4PYgAscHUPdwAsACoOsnCFAXsPmQAEANp/zwDuABMGLwDBcCEP0AAGAecIlHCQAOwPbgDEAUp/rQBwAPUNegBrcHQN+wAVAVsPp3A8AZkExgCUAHl/lwBQAPgPCABRcYMPfQBWATcPRHBmAOEPEAGQAXt8cwBtAfEMTwAWcGoOrggq5D/o+ANoC/v0bX0cbzOI9Pue/bb7p4inDmuKrXFxcYf0hPIEm3759HtEgl9n7Yv2CdedAP0Efmj3dfzN8c8VTHmM67Lo8ehbQfObR3yWghto+Qugi57tXYbxkwuSp4/rA7P1YX7v/2p8iQKw26L03F2XbLsBLC4IK7QdZ2w7AdZtkX4kd2Ng2P4aBXdxpJFEc9wPWwgzC7L3mOE4EcEDQoQD+XOfqHlJejL4TPoYf6T3ChcbHi4Tp3uPhzd6AXNXDEd0G/iqCV5/q4SyiJJ82/n3/wLrbPnsk5aBCAId2qyC8KWehsMNyOUYYph/jYbFl/sJVHorD57viYGAitD/iIEda9b4hAUcY1f78QTa9JvBS3jDAHoQnY13Bl/30Pma8bLdnBjkcndbkoKr/7r3xYu38moDYQKbG3Yrc3wy+U934r3\n" +
                "Pin=1\tNo=1\tIndex=0\tValid=1\tDuress=0\tType=1\tMajorVer=10\tMinorVer=0\tFormat=ZK\tTmp=";
        String[] records = bodyData.trim().replace("BIODATA", "").split("\n");

        //如果多行多个数据需要采用这个方式
        List<Map<String, String>> dataList = new ArrayList<>();

        for (String line : records) {
            // BIODATA 的特殊性：它是键值对格式，需要先收集所有键值
            Map<String, String> currentRecord = new HashMap<>();
            if (line.isEmpty()) continue;
            System.out.println("list data:" + line);
            String[] cols = line.trim().split("\t");
            for (String col : cols) {
                if (col.isEmpty()) continue;

                // 2. 按等号 (=) 分割键值对
                String[] parts = col.split("=", 2);
                if (parts.length == 2) {
                    currentRecord.put(parts[0].trim(), parts[1].trim());
                } else {
                    // 如果一行不是键值对，可能是多行模板数据（但您的示例是一行）
                }
                System.out.println("current col : " + col);
            }
            System.out.println("currentRecord : " + currentRecord);
            dataList.add(currentRecord);
        }
        System.out.println("if->:" + dataList.size());
        System.out.println("my " + dataList);
    }*/

}
