package com.manju.platform.service;

import com.manju.platform.common.Constants;
import com.manju.platform.dao.UsageLogDao;
import com.manju.platform.dao.UserDao;
import com.manju.platform.dto.ParseScriptResponse;
import com.manju.platform.entity.UsageLog;
import com.manju.platform.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class ParseService {
    @Autowired
    private UserDao userDao;
    @Autowired
    private UsageLogDao logDao;
    @Autowired
    private AIService aiService;        // 注入 AI 服务，用于真实调用
    @Autowired
    private ObjectMapper objectMapper;  // 用于解析 JSON


    /**
     * 拆解剧本
     */

    @Transactional(rollbackFor = Exception.class)   // 表示任何异常都回滚，包括检查型异常。
    public ParseScriptResponse parseScript(int userId, String userScript) {
        // 1.检查用户是否存在
        User user = userDao.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        // 尝试插入免费日志（如果今日未免费过，插入成功；否则唯一冲突返回false）
        UsageLog freeLog = new UsageLog();
        freeLog.setUserId(userId);
        freeLog.setToolName(Constants.TOOL_PARSE_SCRIPT);
        freeLog.setIsFree(1);
        freeLog.setPointsDeduct(0);
        freeLog.setCallStatus(1);
        freeLog.setFailReason(null);
        boolean freeInserted = logDao.insert(freeLog);

        if(freeInserted) {
            // 免费成功，调用 AI 拆解剧本，获取原始 JSON 字符串
            String rawJson = aiService.parseScript(userScript);
            System.out.println("原始 rawJson: " + rawJson);  // 打印原始内容

            // 提取第一个 '{' 到最后一个 '}' 之间的内容
            int start = rawJson.indexOf('{');
            int end = rawJson.lastIndexOf('}');
            if (start >= 0 && end > start) {
                String cleanedJson = rawJson.substring(start, end + 1);
                System.out.println("清理后的 cleanedJson: " + cleanedJson);
                try {
                    return objectMapper.readValue(cleanedJson, ParseScriptResponse.class);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("解析拆解剧本结果失败", e);
                }
            } else {
                throw new RuntimeException("无法从AI响应中提取JSON");
            }
        }else{
            // 今日已免费，走付费流程
            // 付费使用：检查积分
            if (user.getPoints() < Constants.POINTS_PARSE_SCRIPT) {
                // 积分不足，记录失败日志
                UsageLog failLog = new UsageLog();
                failLog.setUserId(userId);
                failLog.setToolName(Constants.TOOL_PARSE_SCRIPT);
                failLog.setIsFree(0);
                failLog.setPointsDeduct(0);
                failLog.setCallStatus(0);
                failLog.setFailReason("积分不足，需要" + Constants.POINTS_PARSE_SCRIPT + "分");
                logDao.insert(failLog);
                throw new RuntimeException("积分不足");
            }
            // 乐观锁扣积分（返回新积分，但不需要使用）
            int newPoints = deductPointsWithRetry(userId, Constants.POINTS_PARSE_SCRIPT, 3);
            // 记录付费成功日志
            UsageLog successLog = new UsageLog();
            successLog.setUserId(userId);
            successLog.setToolName(Constants.TOOL_PARSE_SCRIPT);
            successLog.setIsFree(0);
            successLog.setPointsDeduct(Constants.POINTS_PARSE_SCRIPT);
            successLog.setCallStatus(1);
            successLog.setFailReason(null);
            logDao.insert(successLog);

            // 付费成功，调用 AI 拆解剧本，获取原始 JSON 字符串
            String rawJson = aiService.parseScript(userScript);
            System.out.println("原始 rawJson: " + rawJson);  // 打印原始内容

            // 提取第一个 '{' 到最后一个 '}' 之间的内容
            int start = rawJson.indexOf('{');
            int end = rawJson.lastIndexOf('}');
            if (start >= 0 && end > start) {
                String cleanedJson = rawJson.substring(start, end + 1);
                System.out.println("清理后的 cleanedJson: " + cleanedJson);
                try {
                    return objectMapper.readValue(cleanedJson, ParseScriptResponse.class);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("解析拆解剧本结果失败", e);
                }
            } else {
                throw new RuntimeException("无法从AI响应中提取JSON");
            }
        }
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
