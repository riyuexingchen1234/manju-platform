package com.manju.platform.service;

import com.manju.platform.dao.HistoryDao;
import com.manju.platform.entity.UserHistory;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 历史记录服务类
 * 统一处理已登录用户（数据库）和未登录用户（Redis）的历史记录保存与更新
 */
@Service
public class HistoryService {

    @Autowired
    private HistoryDao historyDao;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String GUEST_HISTORY_PREFIX = "guest:history:";
    private static final long GUEST_HISTORY_TTL = 24 * 60 * 60;

    /**
     * 统一保存历史记录
     */
    public UserHistory save(Integer userId, HttpSession session, String toolType, String inputPreview,
                            String resultType, String resultText, String resultUrl,
                            String status, String taskId) {

        UserHistory history = new UserHistory();
        history.setToolType(toolType);
        history.setInputPreview(inputPreview);
        history.setResultType(resultType);
        history.setResultText(resultText);
        history.setResultUrl(resultUrl);
        history.setStatus(status);
        history.setTaskId(taskId);

        if (userId != null) {
            history.setUserId(userId);
            historyDao.insert(history);
            return history;
        } else {
            history.setUserId(0);
            String sessionId = session.getId();
            String redisKey = buildRedisKey(sessionId);
            String hashKey = taskId != null ? taskId : String.valueOf(System.currentTimeMillis());
            redisTemplate.opsForHash().put(redisKey, hashKey, history);
            redisTemplate.expire(redisKey, GUEST_HISTORY_TTL, TimeUnit.SECONDS);
            return history;
        }
    }

    /**
     * 根据 taskId 更新历史记录状态（支持登录/未登录）
     */
    public void updateByTaskId(HttpSession session, String taskId, String status, String videoUrl, String resultText) {
        Integer userId = (Integer) session.getAttribute("userId");

        if (userId != null) {
            UserHistory history = historyDao.findByTaskId(taskId);
            if (history != null && "pending".equals(history.getStatus())) {
                history.setStatus(status);
                history.setResultUrl(videoUrl);
                history.setResultText(resultText);
                historyDao.updateById(history);
            }
        } else {
            String sessionId = session.getId();
            String redisKey = buildRedisKey(sessionId);
            Object obj = redisTemplate.opsForHash().get(redisKey, taskId);
            if (obj instanceof UserHistory) {
                UserHistory h = (UserHistory) obj;
                if ("pending".equals(h.getStatus())) {
                    h.setStatus(status);
                    h.setResultUrl(videoUrl);
                    h.setResultText(resultText);
                    redisTemplate.opsForHash().put(redisKey, taskId, h);
                    redisTemplate.expire(redisKey, GUEST_HISTORY_TTL, TimeUnit.SECONDS);
                }
            }
        }
    }

    /**
     * 根据 taskId 查询历史记录（支持登录/未登录）
     */
    public UserHistory findByTaskId(String taskId) {
        Integer userId = null; // 这里不需要从session获取，因为HistoryDao.findByTaskId已经可以查
        return historyDao.findByTaskId(taskId);
    }

    /**
     * 获取未登录用户的历史记录
     */
    public List<UserHistory> getGuestHistory(HttpSession session) {
        String sessionId = session.getId();
        String redisKey = buildRedisKey(sessionId);
        List<Object> values = redisTemplate.opsForHash().values(redisKey);

        List<UserHistory> historyList = new ArrayList<>();
        if (values != null) {
            for (Object obj : values) {
                if (obj instanceof UserHistory) {
                    historyList.add((UserHistory) obj);
                }
            }
        }
        return historyList;
    }

    /**
     * 登录时将未登录试用历史从 Redis 合并到数据库
     */
    public int mergeGuestHistoryToUser(HttpSession session, int userId) {
        String sessionId = session.getId();
        String redisKey = buildRedisKey(sessionId);
        List<Object> rawList = redisTemplate.opsForHash().values(redisKey);

        int mergedCount = 0;
        if (rawList != null && !rawList.isEmpty()) {
            for (Object obj : rawList) {
                if (obj instanceof UserHistory) {
                    UserHistory h = (UserHistory) obj;
                    h.setUserId(userId);
                    historyDao.insert(h);
                    mergedCount++;
                }
            }
            redisTemplate.delete(redisKey);
        }
        return mergedCount;
    }

    // ======================== 私有方法 ========================

    private String buildRedisKey(String sessionId) {
        return GUEST_HISTORY_PREFIX + sessionId;
    }
}
