package com.manju.platform.service;

import com.manju.platform.common.Constants;
import com.manju.platform.dto.ScriptGenerateResponse;
import com.manju.platform.entity.*;
import com.manju.platform.dao.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
// 这个注解加在方法上，Spring 会在方法开始前开启一个数据库事务，方法执行中所有数据库操作都在同一个事务里，
// 如果方法抛出异常（默认是运行时异常），事务就会回滚，前面的修改都会撤销。

@Service
public class ScriptService {

    @Autowired
    private UserDao userDao;
    @Autowired
    private UsageLogDao logDao;
    @Autowired
    private AIService aiService;        // 注入 AI 服务，用于真实调用

    /**
     * 剧本生成
     *
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)   // 表示任何异常都回滚，包括检查型异常。
    public ScriptGenerateResponse generateScript(int userId, List<Map<String, String>> messages) {
        // 1.检查用户是否存在
        User user = userDao.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        // 先尝试插入免费日志，利用数据库唯一约束自动判断今日是否已免费“只有第一次插入能成功”
        // 如果插入成功（返回 true），则走免费流程；
        // 如果插入失败（返回 false，因为唯一冲突），则说明今日已经免费过了，走付费流程。
        UsageLog freeLog = new UsageLog();
        freeLog.setUserId(userId);
        freeLog.setToolName(Constants.TOOL_SCRIPT_GENERATE);
        freeLog.setIsFree(1);
        freeLog.setPointsDeduct(0);
        freeLog.setCallStatus(1);
        freeLog.setFailReason(null);
        boolean freeInserted=logDao.insert(freeLog);         // 唯一约束冲突会抛出异常

        if(freeInserted) {
                // 插入成功 → 今日免费次数未用
                // 免费流程：直接调用 AI，不扣积分
                String aiResult = aiService.generateScript(messages);
                ScriptGenerateResponse response = new ScriptGenerateResponse();
                response.setContent(aiResult);
                return response;
            }else {
            // 插入失败（唯一约束冲突）→ 今日已免费使用过，进入付费流程
            // 检查积分
            if (user.getPoints() < Constants.POINTS_SCRIPT_GENERATE) {
                // 积分不足，记录失败日志
                UsageLog failLog = new UsageLog();
                failLog.setUserId(userId);
                failLog.setToolName(Constants.TOOL_SCRIPT_GENERATE);
                failLog.setIsFree(0);
                failLog.setPointsDeduct(0);
                failLog.setCallStatus(0);
                failLog.setFailReason("积分不足，需要" + Constants.POINTS_SCRIPT_GENERATE + "分");
                logDao.insert(failLog);
                throw new RuntimeException("积分不足");
            }
            // 乐观锁扣积分（返回新积分，但不需要使用）
            int newPoints = deductPointsWithRetry(userId, Constants.POINTS_SCRIPT_GENERATE, 3);
            // 记录付费成功日志
            UsageLog successLog = new UsageLog();
            successLog.setUserId(userId);
            successLog.setToolName(Constants.TOOL_SCRIPT_GENERATE);
            successLog.setIsFree(0);
            successLog.setPointsDeduct(Constants.POINTS_SCRIPT_GENERATE);
            successLog.setCallStatus(1);
            successLog.setFailReason(null);
            logDao.insert(successLog);
            // 调用 AI 生成剧本
            String aiResult = aiService.generateScript(messages);
            // 封装响应
            ScriptGenerateResponse response = new ScriptGenerateResponse();
            response.setContent(aiResult);
            return response;
        }
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
    private int deductPointsWithRetry(int userId, int pointsToDeduct, int maxRetries) {
        int retry = maxRetries;     // 初始化剩余重试次数
        // while循环：只要还有重试次数，就继续尝试更新
        while (retry-- > 0) {
            // 1. 先从数据库读取当前用户信息（同时拿到当前的version版本号）
            User user = userDao.findById(userId);
            // 2. 校验用户是否存在
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }
            // 3. 校验积分是否足够
            if (user.getPoints() < pointsToDeduct) {
                throw new RuntimeException("积分不足");
            }
            // 4. 计算扣减后的新积分
            int newPoints = user.getPoints() - pointsToDeduct;
            // 5. 调用乐观锁方法更新积分（传入当前读取到的version版本号）
            boolean updated = userDao.updatePointsWithVersion(userId, newPoints, user.getVersion());
            // 6. 判断更新是否成功
            if (updated) {
                // 更新成功：直接返回新积分，结束方法
                return newPoints;
            }
            // 7. 更新失败：说明版本号不匹配（有其他线程先更新了），继续下一次循环重试
            // 这里不需要写代码，while循环会自动继续
        }
        // 8. 重试次数用完仍未成功：抛出异常，提示用户稍后重试
        throw new RuntimeException("积分更新失败，请稍后重试");
    }
}

