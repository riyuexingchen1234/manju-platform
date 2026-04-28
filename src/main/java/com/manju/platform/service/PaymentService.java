package com.manju.platform.service;

import com.manju.platform.common.Constants;
import com.manju.platform.dao.UsageLogDao;
import com.manju.platform.dao.UserDao;
import com.manju.platform.entity.UsageLog;
import com.manju.platform.entity.User;
import com.manju.platform.exception.BusinessException;
import com.manju.platform.functional.AICallable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
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
     * 处理工具调用的支付逻辑（免费/付费）
     */
    @Transactional(rollbackFor = Exception.class)
    public Object processPayment(int userId, String toolName, int pointsCost, AICallable aiCall){
        User user = userDao.findById(userId);
        if (user == null){
            throw new BusinessException("用户不存在");
        }
        // ========== 免费流程 ==========
        try {
            // 先尝试插入免费日志，利用数据库唯一约束自动判断今日是否已免费“只有第一次插入能成功”
            // 如果插入成功（返回 true），则走免费流程；
            // 如果插入失败（返回 false，因为唯一冲突），则说明今日已经免费过了，走付费流程。
            UsageLog freeLog = new UsageLog();
            freeLog.setUserId(userId);
            freeLog.setToolName(toolName);
            freeLog.setIsFree(1);
            freeLog.setPointsDeduct(0);
            freeLog.setCallStatus(0);       // 0 表示处理中or未成功
            freeLog.setFailReason(null);
            int logId = logDao.insertAndReturnId(freeLog);  // 如果今日已有免费记录，会抛 DuplicateKeyException

            // 调用AI
            Object result = aiCall.execute();
            // AI 调用成功，更新日志状态为成功
            logDao.updateLogStatus(logId,1,null);
            return result;
        }catch (DuplicateKeyException e ){
            // 今日已经有免费记录（无论成功还是待确认），走付费流程
        }catch (Exception e){
            // AI 调用失败（包括超时等），事务会回滚，免费日志不会被提交，直接向上抛出异常
            throw e;
        }

        // ========== 付费流程 ==========
        Object aiResult;
        try {
            aiResult = aiCall.execute();
        }catch (Exception e){
            // AI 调用失败，记录失败日志（独立事务，保证失败日志能保存）
            recordFailLog(userId,toolName,"AI调用失败"+e.getMessage());
            throw e;
        }
        // AI 调用成功，检查积分是否足够（再查一次，防止并发扣减）
        user = userDao.findById(userId);
        if (user.getPoints()<pointsCost){
            recordFailLog(userId,toolName,"积分不足，需要"+pointsCost+"分");
            throw new BusinessException("积分不足");
        }

        // 扣积分（带乐观锁重试）（返回新积分，但不需要使用）
        int newPoints = deductPointsWithRetry(userId,pointsCost,3,toolName);
        // 记录付费成功日志
        UsageLog successLog = new UsageLog();
        successLog.setUserId(userId);
        successLog.setToolName(toolName);
        successLog.setIsFree(0);
        successLog.setPointsDeduct(pointsCost);
        successLog.setCallStatus(1);
        successLog.setFailReason(null);
        logDao.insertAndReturnId(successLog);

        return aiResult;
    }
    // 独立事务记录失败日志（使用 REQUIRES_NEW 并降低隔离级别，避免锁等待）
    @Transactional(propagation = Propagation.REQUIRES_NEW,isolation = Isolation.READ_UNCOMMITTED)
    // 这个注解加在方法上，Spring 会在方法开始前开启一个数据库事务，方法执行中所有数据库操作都在同一个事务里，
    // 如果方法抛出异常（默认是运行时异常），事务就会回滚，前面的修改都会撤销。
    public void recordFailLog(int userId,String toolName, String reason){
        UsageLog failLog = new UsageLog();
        failLog.setUserId(userId);
        failLog.setToolName(toolName);
        failLog.setIsFree(0);
        failLog.setPointsDeduct(0);
        failLog.setCallStatus(0);
        failLog.setFailReason(reason);
        logDao.insertAndReturnId(failLog);
    }
    /**
     * 带重试机制的乐观锁积分扣减
     * 作用：在乐观锁的基础上增加重试逻辑，解决乐观锁并发冲突时更新失败率高的问题
     * 核心逻辑：
     * - 每次尝试：先读取用户信息 → 校验积分 → 用乐观锁更新
     * - 如果乐观锁更新失败（版本号不匹配），说明有其他线程先更新了，就重试一次
     * - 直到更新成功或达到最大重试次数
     *
     * @param userId         要扣积分的用户ID
     * @param pointsToDeduct 要扣除的积分数量（如：10）
     * @param maxRetries     最大重试次数（如：3，表示最多尝试3次）
     * @return 扣减成功后的新积分余额
     * @throws RuntimeException 如果用户不存在、积分不足或重试次数用完仍失败
     */
    private int deductPointsWithRetry(int userId, int pointsToDeduct, int maxRetries, String toolName) {
        int retry = maxRetries;     // 初始化剩余重试次数
        // while循环：只要还有重试次数，就继续尝试更新
        while (retry-- > 0) {
            // 1. 先从数据库读取当前用户信息（同时拿到当前的version版本号）
            User user = userDao.findById(userId);
            // 2. 校验用户是否存在
            if (user == null) {
                throw new BusinessException("用户不存在");
            }
            // 3. 校验积分是否足够
            if (user.getPoints() < pointsToDeduct) {
                // 积分不足也要记录失败日志
                recordFailLog(userId, toolName, "积分不足");
                throw new BusinessException("积分不足");
            }
            // 4. 计算扣减后的新积分
            int newPoints = user.getPoints() - pointsToDeduct;
            // 5. 调用乐观锁方法更新积分（传入当前读取到的version版本号）
            boolean updated = userDao.updatePointsWithVersion(
                    userId, newPoints, user.getVersion());
            // 6. 判断更新是否成功
            if (updated) {
                // 更新成功：直接返回新积分，结束方法
                return newPoints;
            }
            // 7. 更新失败：说明版本号不匹配（有其他线程先更新了），继续下一次循环重试
            // 这里不需要写代码，while循环会自动继续
        }
        // 8. 重试3次仍未成功：记录失败日志并抛出异常，提示用户稍后重试
        recordFailLog(userId, toolName,"乐观锁重试失败，请稍后重试");
        throw new BusinessException("积分更新失败，请稍后重试");
    }
}
