package com.manju.platform.service;

import com.manju.platform.common.Constants;
import com.manju.platform.dao.UsageLogDao;
import com.manju.platform.dao.UserDao;
import com.manju.platform.dto.VideoGenerateRequest;
import com.manju.platform.dto.VideoGenerateResponse;
import com.manju.platform.entity.UsageLog;
import com.manju.platform.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class VideoService {
    @Autowired
    private UserDao userDao;
    @Autowired
    private AIService aiService;
    @Autowired
    private UsageLogDao logDao;
    @Transactional(rollbackFor = Exception.class)
    public VideoGenerateResponse generateVideo(VideoGenerateRequest req) {
        // 1. 检查用户
        User user = userDao.findById(req.getUserId());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        // 2. 检查今日是否已免费使用
        boolean freeUsed = logDao.isFreeUsedToday(req.getUserId(), Constants.TOOL_VIDEO_GENERATE);
        if (!freeUsed) {
            // 免费使用：插入免费日志，不扣积分
            UsageLog freeLog = new UsageLog();
            freeLog.setUserId(req.getUserId());
            freeLog.setToolName(Constants.TOOL_VIDEO_GENERATE);
            freeLog.setIsFree(1);
            freeLog.setPointsDeduct(0);
            freeLog.setCallStatus(1);
            freeLog.setFailReason(null);
            logDao.insert(freeLog);
        } else {
            // 付费使用：检查积分
            if (user.getPoints() < Constants.POINTS_VIDEO_GENERATE) {
                UsageLog failLog = new UsageLog();
                failLog.setUserId(req.getUserId());
                failLog.setToolName(Constants.TOOL_VIDEO_GENERATE);
                failLog.setIsFree(0);
                failLog.setPointsDeduct(0);
                failLog.setCallStatus(0);
                failLog.setFailReason("积分不足，需要" + Constants.POINTS_VIDEO_GENERATE + "分");
                logDao.insert(failLog);
                throw new RuntimeException("积分不足");
            }
            // 乐观锁扣积分（返回新积分，但不需要使用）
            int newPoints = deductPointsWithRetry(req.getUserId(), Constants.POINTS_VIDEO_GENERATE, 3);

            // 记录付费成功日志
            UsageLog successLog = new UsageLog();
            successLog.setUserId(req.getUserId());
            successLog.setToolName(Constants.TOOL_VIDEO_GENERATE);
            successLog.setIsFree(0);
            successLog.setPointsDeduct(Constants.POINTS_VIDEO_GENERATE);
            successLog.setCallStatus(1);
            successLog.setFailReason(null);
            logDao.insert(successLog);
        }

        // 3. 调用阿里云AI创建视频生成任务
        Map<String, String> taskInfo = aiService.createVideoGenerationTask(
                req.getKeyframeImageUrl(),
                req.getDescription()
        );

        // 4. 构建响应
        VideoGenerateResponse response = new VideoGenerateResponse();
        response.setTaskId(taskInfo.get("taskId"));
        response.setStatus(taskInfo.get("status"));
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
