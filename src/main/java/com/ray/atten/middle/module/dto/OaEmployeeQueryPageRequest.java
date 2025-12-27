package com.ray.atten.middle.module.dto;

import lombok.*;

import java.io.Serializable;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OaEmployeeQueryPageRequest implements Serializable {

    // --- 模糊查詢條件 (支持 pin 或 name 模糊匹配) ---
    private String keyword;

    // --- 級聯查詢條件 (未來擴展用) ---
    // 建議使用 ID 而不是名稱，以確保準確性
    private String companyId; // 未來用於分公司篩選
    private String deptId;    // 未來用於部門篩選
    private Boolean inService; // 在職狀態篩選

    // --- 分頁查詢條件 ---
    private int pageNum = 1;      // 當前頁碼 (默認第 1 頁)
    private int pageSize = 10;    // 每頁記錄數 (默認 10 條)

    /**
     * 排序字段：允許的值包括 pin, inService, entryDate, createTime。
     * 默認為空，如果為空則不排序或使用數據庫默認順序。
     */
    private String sortBy;

    /**
     * 排序方向：允許的值為 ASC (升序) 或 DESC (降序)。
     * 默認為 DESC。
     */
    private String sortOrder = "DESC";

}
