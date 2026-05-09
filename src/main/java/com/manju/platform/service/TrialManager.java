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
        logger.debug("记录试用: sessionId={}, tool={}, date={}", session.getId(), toolName, today);
    }

    /**
     * 获取试用记录 Map（从 Session 获取，不存在则创建）
     */
    public Map<String, String> getTrialDateMap(HttpSession session) {
        Map<String, String> trialDateMap = (Map<String, String>) session.getAttribute(SESSION_KEY);
        if (trialDateMap == null) {
            trialDateMap = new HashMap<>();
        }
        return trialDateMap;
    }

    /**
     * 构建试用记录 key：toolName_date
     */
    private String buildKey(String toolName, String date) {
        return toolName + "_" + date;
    }
}
