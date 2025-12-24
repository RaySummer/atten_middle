package com.ray.atten.middle.module.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;

import java.util.Map;

public class ExcelReader {

    public static void main(String[] args) {
        // 1. Excel 文件路径
        String fileName = "C:\\Users\\Ray\\Downloads\\oaEmployee.xlsx";
        // 2. 表名
        String tableName = "oa_employee";

        // 3. 开始读取
        EasyExcel.read(fileName, new ReadListener<Map<Integer, String>>() {
            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {
                // data 是一个 Map，Key 是列号（从 0 开始），Value 是单元格内容
                // 假设提取第 0 列 (姓名) 和 第 1 列 (年龄)
                String name = data.get(0);
                String pin = data.get(1);
                String dept = data.get(2);
                String company = data.get(3);
                String officeLocation = data.get(5);
                String entryTime = data.get(6);

                // 4. 拼装 SQL
                String sql = String.format(
                        "INSERT INTO %s (name, pin, dept, company, in_service, office_location, entry_date, create_time, update_time)" +
                                " VALUES ('%s', '%s', '%s', '%s', 't', '%s', '%s', NOW(), NOW());",
                        tableName, name, pin, dept, company, officeLocation, entryTime
                );

                System.out.println(sql);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                System.out.println("--- 读取完成 ---");
            }
        }).sheet().doRead();
    }

}
