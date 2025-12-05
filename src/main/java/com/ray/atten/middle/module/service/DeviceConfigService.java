package com.ray.atten.middle.module.service;

import com.ray.atten.middle.module.model.OperationLog;
import com.ray.atten.middle.module.repository.OperationLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeviceConfigService {

    @Autowired
    private OperationLogRepository operationLogRepository;

    public OperationLog getLastOneLogByOperation(Integer operation, String sn) {
        OperationLog lastTimeByOperation = operationLogRepository.findLastTimeByOperation(operation, sn);
        return lastTimeByOperation;
    }

}
