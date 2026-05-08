package com.manju.platform.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 未登录用户试用管理器
 * 统一处理 Session 级别的试用次数限制（每日每工具一次）
 */
@Component
public class TrialManager {
    private static final Logger logger = LoggerFactory.getLogger(TrialManager.class);

    private static final String SESSION_KEY = "trialDateMap";

    /**
     * 存储 sessionId -> trialDateMap 的映射，用于延迟试用判断
     * key: sessionId, value: 该session的试用记录Map
     */
    private final Map<String, Map<String, String>> sessionTrialMap = new HashMap<>();

    /**
     * 检查未登录用户是否还可以试用指定工具（今日尚未试用）
     *
     * @param session  当前 Session
     * @param toolName 工具名，如 "script_generate"
     * @return true 可以试用；false 今日已试用过
     */
    public boolean canTrial(HttpSession session, String toolName) {
        Map<String, String> trialDateMap = getTrialDateMap(session);
        String today = LocalDate.now().toString();
        String trialKey = buildKey(toolName, today);
        return !trialDateMap.containsKey(trialKey);
    }

    /**
     * 根据 sessionId 检查是否还可以试用指定工具（延迟试用场景）
     *
     * @param sessionId Session ID
     * @param toolName  工具名
     * @return true 可以试用；false 今日已试用过
     */
    public boolean canTrialBySessionId(String sessionId, String toolName) {
        Map<String, String> trialDateMap = sessionTrialMap.get(sessionId);
        if (trialDateMap == null) {
            return true; // 没有试用记录，可以试用
        }
        String today = LocalDate.now().toString();
        String trialKey = buildKey(toolName, today);
        return !trialDateMap.containsKey(trialKey);
    }

    /**
     * 记录已试用（在 Session 中标记今日已试用该工具）
     *
     * @param session  当前 Session
     * @param toolName 工具名
     */
    public void recordTrial(HttpSession session, String toolName) {
        Map<String, String> trialDateMap = getTrialDateMap(session);
        String today = LocalDate.now().toString();
        String trialKey = buildKey(toolName, today);
        trialDateMap.put(trialKey, today);
        session.setAttribute(SESSION_KEY, trialDateMap);
        // 同步到 sessionTrialMap
        sessionTrialMap.put(session.getId(), trialDateMap);
        logger.debug("记录试用: sessionId={}, tool={}, date={}", session.getId(), toolName, today);
    }

    /**
     * 根据 sessionId 记录已试用（延迟试用场景）
     *
     * @param session  当前 Session（用于更新 sessionAttribute）
     * @param toolName 工具名
     */
    public void recordTrialBySessionId(HttpSession session, String toolName) {
        Map<String, String> trialDateMap = sessionTrialMap.get(session.getId());
        if (trialDateMap == null) {
            trialDateMap = new HashMap<>();
            sessionTrialMap.put(session.getId(), trialDateMap);
        }
        String today = LocalDate.now().toString();
        String trialKey = buildKey(toolName, today);
        trialDateMap.put(trialKey, today);
        session.setAttribute(SESSION_KEY, trialDateMap);
        logger.debug("记录延迟试用: sessionId={}, tool={}, date={}", session.getId(), toolName, today);
    }

    /**
     * 清除未登录用户的试用记录（登录时调用）
     *
     * @param session 当前 Session
     */
    public void clearTrials(HttpSession session) {
        session.removeAttribute(SESSION_KEY);
        sessionTrialMap.remove(session.getId());
        logger.debug("清除试用记录: sessionId={}", session.getId());
    }

    // ======================== 私有方法 ========================

    /**
     * 从 Session 获取试用记录 Map，不存在则创建
     */
    private Map<String, String> getTrialDateMap(HttpSession session) {
        String sessionId = session.getId();
        Map<String, String> trialDateMap = (Map<String, String>) session.getAttribute(SESSION_KEY);
        if (trialDateMap == null) {
            trialDateMap = new HashMap<>();
            // 如果 sessionTrialMap 中有记录，则复用
            Map<String, String> storedMap = sessionTrialMap.get(sessionId);
            if (storedMap != null) {
                trialDateMap = storedMap;
            }
        }
        // 同步到 sessionTrialMap
        sessionTrialMap.put(sessionId, trialDateMap);
        return trialDateMap;
    }

    /**
     * 构建试用记录 key：toolName_date
     */
    private String buildKey(String toolName, String date) {
        return toolName + "_" + date;
    }
}
