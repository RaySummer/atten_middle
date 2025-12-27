package com.ray.atten.middle.module.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceLogRequest extends BaseDto implements Serializable {

    private String keyword;
    private String startTime;
    private String endTime;

    // --- 分頁查詢條件 ---
    private int pageNum = 1;      // 當前頁碼 (默認第 1 頁)
    private int pageSize = 10;    // 每頁記錄數 (默認 10 條)

    /**
     * 排序字段：允許的值包括 pin,verifyTime
     * 默認為空，如果為空則不排序或使用verifyTime desc排序。
     */
    private String sortBy;

    /**
     * 排序方向：允許的值為 ASC (升序) 或 DESC (降序)。
     * 默認為 DESC。
     */
    private String sortOrder = "DESC";

}
