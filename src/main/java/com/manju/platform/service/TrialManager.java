package com.manju.platform.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 未登录用户试用管理器
 * 使用Redis存储试用记录，实现Session持久化
 */
@Component
public class TrialManager {
    private static final Logger logger = LoggerFactory.getLogger(TrialManager.class);
    private static final String KEY_PREFIX = "trial:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 检查未登录用户是否还可以试用指定工具（今日尚未试用）
     *
     * @param session  当前 Session
     * @param toolName 工具名，如 "script_generate"
     * @return true 可以试用；false 今日已试用过
     */
    public boolean canTrial(HttpSession session, String toolName) {
        String key = buildKey(session.getId(), toolName);
        boolean canTrial = !Boolean.TRUE.equals(redisTemplate.hasKey(key));
        logger.debug("检查试用: sessionId={}, tool={}, canTrial={}", session.getId(), toolName, canTrial);
        return canTrial;
    }

    /**
     * 记录已试用（在Redis中标记今日已试用该工具）
     *
     * @param session  当前 Session
     * @param toolName 工具名
     */
    public void recordTrial(HttpSession session, String toolName) {
        String key = buildKey(session.getId(), toolName);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endOfDay = now.toLocalDate().atTime(23, 59, 59);
        long secondsUntilEndOfDay = Duration.between(now, endOfDay).getSeconds();
        if (secondsUntilEndOfDay <= 0) {
            secondsUntilEndOfDay = 86400;
        }

        redisTemplate.opsForValue().set(key, LocalDate.now().toString(), Duration.ofSeconds(secondsUntilEndOfDay));
        logger.debug("记录试用: sessionId={}, tool={}, 过期剩余{}秒", session.getId(), toolName, secondsUntilEndOfDay);
    }

    /**
     * 构建Redis key：trial:sessionId:toolName
     */
    private String buildKey(String sessionId, String toolName) {
        return KEY_PREFIX + sessionId + ":" + toolName;
    }
}
