package com.manju.platform.service;

import com.manju.platform.dao.UsageLogDao;
import com.manju.platform.dao.UserDao;
import com.manju.platform.entity.UsageLog;
import com.manju.platform.entity.User;
import com.manju.platform.exception.BusinessException;
import com.manju.platform.functional.AICallable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    @Autowired
    private UserDao userDao;
    @Autowired
    private UsageLogDao logDao;

    /**
     * 处理工具调用的支付逻辑（同步AI：先扣后调，失败退还）
     * 适用于：剧本生成、角色生成、场景生成、关键帧生成
     */
    @Transactional(rollbackFor = Exception.class)
    public <T> T processPayment(int userId, String toolName, int pointsCost, AICallable<T> aiCall) {
        // ========== 免费流程 ==========
        try {
            // 先尝试插入免费日志，利用数据库唯一约束自动判断今日是否已免费
            // 只有第一次插入能成功；如果抛 DuplicateKeyException，说明今日已经免费过了
            UsageLog freeLog = new UsageLog();
            freeLog.setUserId(userId);
            freeLog.setToolName(toolName);
            freeLog.setIsFree(1);
            freeLog.setPointsDeduct(0);
            freeLog.setCallStatus(0);
            freeLog.setFailReason(null);
            int logId = logDao.insertAndReturnId(freeLog);

            // 调用AI
            T result = aiCall.execute();
            // AI 调用成功，更新日志状态为成功
            logDao.updateLogStatus(logId, 1, null);
            return result;
        } catch (DuplicateKeyException e) {
            // 今日已经有免费记录（无论成功还是待确认），走付费流程
        } catch (Exception e) {
            // AI 调用失败（包括超时等），事务会回滚，免费日志不会被提交，直接向上抛出异常
            throw e;
        }

        // ========== 付费流程：先扣后调，失败退还 ==========
        // 1. 查询用户积分
        User user = userDao.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 2. 检查积分是否足够
        if (user.getPoints() < pointsCost) {
            recordFailLog(userId, toolName, "积分不足，需要" + pointsCost + "分");
            throw new BusinessException("积分不足");
        }
        // 3. 预扣积分（乐观锁）
        int newPoints = user.getPoints() - pointsCost;
        boolean updated = userDao.updatePointsWithVersion(userId, newPoints, user.getVersion());
        if (!updated) {
            throw new BusinessException("积分扣费失败，请重试");
        }
        // 4. 执行AI调用
        try {
            T result = aiCall.execute();
            // 5. AI调用成功，记录付费日志
            UsageLog successLog = new UsageLog();
            successLog.setUserId(userId);
            successLog.setToolName(toolName);
            successLog.setIsFree(0);
            successLog.setPointsDeduct(pointsCost);
            successLog.setCallStatus(1);
            successLog.setFailReason(null);
            logDao.insertAndReturnId(successLog);
            return result;
        } catch (Exception e) {
            // 6. AI调用失败，退还积分
            userDao.refundPoints(userId, pointsCost);
            // 记录失败日志
            recordFailLog(userId, toolName, "AI调用失败：" + e.getMessage());
            throw e;
        }
    }

    /**
     * 延迟扣费（异步AI：视频成功后调用）
     * 适用于：视频生成
     */
    @Transactional(rollbackFor = Exception.class)
    public void deferredProcessPayment(int userId, String toolName, int pointsCost) {
        // 1. 检查是否可免费
        try {
            UsageLog freeLog = new UsageLog();
            freeLog.setUserId(userId);
            freeLog.setToolName(toolName);
            freeLog.setIsFree(1);
            freeLog.setPointsDeduct(0);
            freeLog.setCallStatus(0);
            freeLog.setFailReason(null);
            logDao.insertAndReturnId(freeLog);
            // 免费成功，直接返回
            return;
        } catch (DuplicateKeyException e) {
            // 今日已有免费记录，走付费流程
        }

        // 2. 付费流程：查积分→扣积分→记录日志
        User user = userDao.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getPoints() < pointsCost) {
            throw new BusinessException("积分不足");
        }
        boolean updated = userDao.updatePointsWithVersion(userId, user.getPoints() - pointsCost, user.getVersion());
        if (!updated) {
            throw new BusinessException("积分扣费失败，请重试");
        }
        // 记录付费日志
        UsageLog paidLog = new UsageLog();
        paidLog.setUserId(userId);
        paidLog.setToolName(toolName);
        paidLog.setIsFree(0);
        paidLog.setPointsDeduct(pointsCost);
        paidLog.setCallStatus(1);
        paidLog.setFailReason(null);
        logDao.insertAndReturnId(paidLog);
    }

    // 独立事务记录失败日志（使用 REQUIRES_NEW 并降低隔离级别，避免锁等待）
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_UNCOMMITTED)
    public void recordFailLog(int userId, String toolName, String reason) {
        UsageLog failLog = new UsageLog();
        failLog.setUserId(userId);
        failLog.setToolName(toolName);
        failLog.setIsFree(0);
        failLog.setPointsDeduct(0);
        failLog.setCallStatus(0);
        failLog.setFailReason(reason);
        logDao.insertAndReturnId(failLog);
    }
}
