package com.manju.platform.service;

import com.manju.platform.common.Result;
import com.manju.platform.entity.*;
import com.manju.platform.dao.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
// 这个注解加在方法上，Spring 会在方法开始前开启一个数据库事务，方法执行中所有数据库操作都在同一个事务里，
// 如果方法抛出异常（默认是运行时异常），事务就会回滚，前面的修改都会撤销。

@Service
public class ToolService {
    private static final int POINTS_PER_CALL = 10;

    @Autowired
    private UserDao userDao;
    @Autowired
    private UsageLogDao logDao;
    @Autowired
    private AIService aiService;        // 注入 AI 服务，用于真实调用

//    // 模拟 AI 生成（后续会替换为真实 AI）
//    private String mockAI(String prompt){
//        return "【模拟 AI】基于提示词『" + prompt +
//                "』生成的剧本：\n场景：雪山\n角色：战士\n台词：哼，别装神秘了！";
//    }

    /**
     * 用户使用工具（免费/付费 + 调用 AI）
     *
     * @param userId   用户ID
     * @param toolName 工具名称
     * @param prompt   AI 提示词
     * @return 统一返回结果，包含 AI 生成内容
     */
    @Transactional(rollbackFor = Exception.class)   // 表示任何异常都回滚，包括检查型异常。
    public Result useTool(int userId, String toolName, String prompt) {
        // 1.检查用户是否存在
        User user = userDao.findById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        // 2.检查今日是否已经免费使用
        boolean freeUsed = logDao.isFreeUsedToday(userId, toolName);
        String aiResult;

        if (!freeUsed) {
            // 免费使用：插入免费日志
            UsageLog log = new UsageLog();
            log.setUserId(userId);
            log.setToolName(toolName);
            log.setIsFree(1);
            log.setPointsDeduct(0);
            log.setCallStatus(1);
            log.setFailReason(null);
            logDao.insert(log);
            // 调用 AI（如果失败会抛出异常，事务回滚）
            aiResult = callAI(prompt);
            return Result.success("免费调用成功", aiResult);
        } else {
            // 付费使用，检查积分
            if (user.getPoints() < POINTS_PER_CALL) {
                // 积分不足，记录失败日志并返回
                UsageLog failLog = new UsageLog();
                failLog.setUserId(userId);
                failLog.setToolName(toolName);
                failLog.setIsFree(0);
                failLog.setPointsDeduct(0);
                failLog.setCallStatus(0);
                failLog.setFailReason("积分不足，当前积分：" + user.getPoints());
                logDao.insert(failLog);
                return Result.fail("积分不足，当前积分" + user.getPoints());
            }
            // 扣积分
            int newPoints = user.getPoints() - POINTS_PER_CALL;
            boolean updated = userDao.updatePoints(userId, newPoints);
            if (!updated) {
                throw new RuntimeException("积分更新失败");
            }
            // 记录成功日志
            UsageLog successLog = new UsageLog();
            successLog.setUserId(userId);
            successLog.setToolName(toolName);
            successLog.setIsFree(0);
            successLog.setPointsDeduct(POINTS_PER_CALL);
            successLog.setCallStatus(1);
            successLog.setFailReason(null);
            logDao.insert(successLog);
            // 调用 AI
            aiResult = callAI(prompt);
            return Result.success("付费调用成功，扣除"
                    + POINTS_PER_CALL + "积分，剩余" + newPoints, aiResult);
        }
    }

    /**
     * 调用 AI 服务
     * 抽取了一个私有方法 callAI，统一处理异常。如果 AI 调用失败，抛出 RuntimeException，事务自动回滚。
     * 成功时，将 AI 返回的字符串放入 Result 的 data 字段。
     */
    private String callAI(String prompt) {
        try {
            return aiService.generateText(prompt);
        } catch (Exception e) {
            // 将任何异常包装为 RuntimeException，确保事务回滚
            throw new RuntimeException("AI服务异常，操作已回滚", e);
        }
    }

}
