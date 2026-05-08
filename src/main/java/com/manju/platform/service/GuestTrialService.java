package com.manju.platform.service;

import com.manju.platform.exception.BusinessException;
import com.manju.platform.functional.AICallable;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 未登录用户试用服务
 * 封装「检查 → 调用 AI → 记录试用」的原子流程。
 * AI 调用失败时不记录试用，用户可重试。
 */
@Service
public class GuestTrialService {

    @Autowired
    private TrialManager trialManager;

    /**
     * 执行试用流程（同步模式：AI调用前检查，调用后记录）
     *
     * @param session     当前 Session
     * @param toolName    工具名（如 "script_generate"）
     * @param displayName 工具中文名（用于错误提示）
     * @param aiCall      AI 调用逻辑
     * @return AI 调用结果
     * @throws BusinessException 如果今日已试用过
     */
    public <T> T execute(HttpSession session, String toolName, String displayName,
                         AICallable<T> aiCall) {
        if (!trialManager.canTrial(session, toolName)) {
            throw new BusinessException("您今日已试用过" + displayName + "，请登录后使用");
        }
        T result = aiCall.execute();
        trialManager.recordTrial(session, toolName);
        return result;
    }

    /**
     * 延迟试用判断（异步模式：视频成功后调用）
     * 根据sessionId判断该用户今日是否已免费试用过该工具，没有则记录试用，有则报错
     *
     * @param sessionId  Session ID（用于关联试用记录）
     * @param toolName   工具名
     * @param session    当前 Session（用于记录试用状态）
     */
    public void deferredTrialCheck(String sessionId, String toolName, HttpSession session) {
        if (!trialManager.canTrialBySessionId(sessionId, toolName)) {
            throw new BusinessException("您今日已试用过该工具，请登录后使用");
        }
        trialManager.recordTrialBySessionId(session, toolName);
    }
}
