package com.manju.platform.service;

import com.manju.platform.dao.UsageLogDao;
import com.manju.platform.entity.UsageLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FailLogService {
    @Autowired
    private UsageLogDao  logDao;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(int userId, String toolName, String reason) {
        UsageLog log = new UsageLog();
        log.setUserId(userId);
        log.setTool(toolName);
        log.setIsFree(0);
        log.setPointsCost(0);
        log.setCallStatus(0);
        log.setFailReason(reason);
        logDao.insertAndReturnId(log);
    }
}
