package com.manju.platform.service;

import com.manju.platform.common.Constants;
import com.manju.platform.dao.UsageLogDao;
import com.manju.platform.dao.UserDao;
import com.manju.platform.dto.CharacterGenerateRequest;
import com.manju.platform.dto.CharacterGenerateResponse;
import com.manju.platform.entity.UsageLog;
import com.manju.platform.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;

@Service
public class CharacterService {
    @Autowired
    private UserDao userDao;
    @Autowired
    private UsageLogDao logDao;
    @Autowired
    private AIService aiService;
    @Transactional(rollbackFor = Exception.class)
    public CharacterGenerateResponse generateCharacter(CharacterGenerateRequest req){
        // 1. 检查用户
        User user = userDao.findById(req.getUserId());
        if (user == null){
            throw new RuntimeException("用户不存在");
        }
        // 先尝试插入免费日志，利用数据库唯一约束自动判断今日是否已免费“只有第一次插入能成功”
        // 如果插入成功（返回 true），则走免费流程；
        // 如果插入失败（返回 false，因为唯一冲突），则说明今日已经免费过了，走付费流程。
        UsageLog freeLog = new UsageLog();
        freeLog.setUserId(req.getUserId());
        freeLog.setToolName(Constants.TOOL_CHARACTER_GENERATE);
        freeLog.setIsFree(1);
        freeLog.setPointsDeduct(0);
        freeLog.setCallStatus(1);
        freeLog.setFailReason(null);
        boolean freeInserted = logDao.insert(freeLog);
        if (freeInserted){
            // 免费流程：调用 AI 生成图片（文生图，无参考图）
            String imageUrl = aiService.generateImageFromMultimodal(
                    req.getCharacterPrompt(),
                    Collections.emptyList()
            );
            // 封装响应
            CharacterGenerateResponse response = new CharacterGenerateResponse();
            response.setImageUrl(imageUrl);
            return response;
        }else {
            // 付费流程：检查积分
            if (user.getPoints() < Constants.POINTS_CHARACTER_GENERATE) {
                // 积分不足，记录失败日志
                UsageLog failLog = new UsageLog();
                failLog.setUserId(req.getUserId());
                failLog.setToolName(Constants.TOOL_CHARACTER_GENERATE);
                failLog.setIsFree(0);
                failLog.setPointsDeduct(0);
                failLog.setCallStatus(0);
                failLog.setFailReason("积分不足，需要" + Constants.POINTS_CHARACTER_GENERATE + "分");
                logDao.insert(failLog);
                throw new RuntimeException("积分不足");
            }
            // 乐观锁扣积分（返回新积分，但不需要使用）
            int newPoints = deductPointsWithRetry(req.getUserId(), Constants.POINTS_CHARACTER_GENERATE, 3);
            // 记录付费成功日志
            UsageLog successLog = new UsageLog();
            successLog.setUserId(req.getUserId());
            successLog.setToolName(Constants.TOOL_CHARACTER_GENERATE);
            successLog.setIsFree(0);
            successLog.setPointsDeduct(Constants.POINTS_CHARACTER_GENERATE);
            successLog.setCallStatus(1);
            successLog.setFailReason(null);
            logDao.insert(successLog);
            // 调用 AI 生成图片（文生图，无参考图）
            String imageUrl = aiService.generateImageFromMultimodal(
                    req.getCharacterPrompt(),
                    Collections.emptyList()
            );
            // 封装响应
            CharacterGenerateResponse response = new CharacterGenerateResponse();
            response.setImageUrl(imageUrl);
            return response;
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
