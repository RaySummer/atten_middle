指令 (CommandContent 存儲)	作用	數據回傳接口
GETUSER	獲取設備上所有員工的基本信息（PIN, 姓名, 卡號, 權限）。	/iclock/cdata (POST)
GETUSERINFO PIN={pin}	獲取特定工號員工的信息。	/iclock/cdata (POST)
GETATTLOG	獲取所有考勤記錄 (ATTLOG)。	/iclock/cdata (POST)
GETBIODATA	獲取所有生物識別數據 (指紋、人臉等)。	/iclock/cdata (POST)
REBOOT	重啟考勤機。	無數據回傳
CHECK	讓設備立即重新檢查是否有新的待處理指令。	無數據回傳
SETOPTION TimeZone=8	設置設備的選項參數。	無數據回傳
CLEARLOG	清除設備上所有的考勤記錄。	無數據回傳
CLEARUSER	清除設備上所有的員工數據。	無數據回傳
SETUSER PIN=...	創建或修改一個員工數據。	無數據回傳


