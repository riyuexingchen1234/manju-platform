package com.manju.platform.service;

import com.manju.platform.common.Constants;
import com.manju.platform.dto.SceneGenerateResponse;
import com.manju.platform.dto.ScriptGenerateResponse;
import com.manju.platform.entity.*;
import com.manju.platform.dao.*;
import org.springframework.beans.factory.annotation.Autowired;
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
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)   // 表示任何异常都回滚，包括检查型异常。
    public ScriptGenerateResponse generateScript(int userId, List<Map<String, String>> messages) {
        // 1.检查用户是否存在
        User user = userDao.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        // 2. 检查今日是否已免费使用过任意工具
        boolean freeUsed = logDao.isFreeUsedToday(userId, Constants.TOOL_SCRIPT_GENERATE);
        if (!freeUsed) {
            // 免费使用：插入免费日志
            UsageLog freeLog = new UsageLog();
            freeLog.setUserId(userId);
            freeLog.setToolName(Constants.TOOL_SCRIPT_GENERATE);
            freeLog.setIsFree(1);
            freeLog.setPointsDeduct(0);
            freeLog.setCallStatus(1);
            freeLog.setFailReason(null);
            logDao.insert(freeLog);
        } else {
            // 付费使用：检查积分
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
        }
        // 调用 AI 生成剧本
        String aiResult = aiService.generateScript(messages);
        // 封装响应
        ScriptGenerateResponse response = new ScriptGenerateResponse();
        response.setContent(aiResult);
        return response;
    }

    private int deductPointsWithRetry(int userId, int pointsToDeduct, int maxRetries) {
        int retry = maxRetries;
        while (retry-- > 0) {
            User user = userDao.findById(userId);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }
            if (user.getPoints() < pointsToDeduct) {
                throw new RuntimeException("积分不足");
            }
            int newPoints = user.getPoints() - pointsToDeduct;
            boolean updated = userDao.updatePointsWithVersion(userId, newPoints, user.getVersion());
            if (updated) {
                return newPoints;
            }
            // 版本冲突，继续重试
        }
        throw new RuntimeException("积分更新失败，请稍后重试");
    }
}

