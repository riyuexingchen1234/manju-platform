package com.manju.platform.service;

import com.manju.platform.dao.HistoryDao;
import com.manju.platform.entity.UserHistory;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 历史记录服务类
 * 所有历史记录统一存储在 MySQL，通过 sessionId 字段追踪未登录会话
 * 登录时将 sessionId 对应的记录归属到真实用户
 */
@Service
public class HistoryService {

    @Autowired
    private HistoryDao historyDao;

    /**
     * 保存历史记录
     * 登录用户：userId=真实用户ID
     * 未登录用户：userId=null，sessionId=当前会话ID
     */
    public UserHistory save(Integer userId, HttpSession session, String tool, String inputPreview,
                            String resultText, String resultUrl,
                            String status, String taskId) {

        UserHistory history = new UserHistory();
        history.setTool(tool);
        history.setInputPreview(inputPreview);
        history.setResultText(resultText);
        history.setResultUrl(resultUrl);
        history.setStatus(status);
        history.setTaskId(taskId);
        history.setSessionId(session.getId());

        if (userId != null) {
            history.setUserId(userId);
        }
        // 未登录时 userId 为 null，由 sessionId 追踪

        historyDao.insert(history);
        return history;
    }

    /**
     * 根据 taskId 更新异步任务状态
     * 通过 taskId 直接定位 MySQL 记录，适用于登录和未登录用户
     */
    public void updateByTaskId(String taskId, String status, String videoUrl, String resultText) {
        UserHistory history = historyDao.findByTaskId(taskId);
        if (history != null && "pending".equals(history.getStatus())) {
            history.setStatus(status);
            history.setResultUrl(videoUrl);
            history.setResultText(resultText);
            historyDao.updateById(history);
        }
    }

    /**
     * 根据 taskId 查询历史记录
     */
    public UserHistory findByTaskId(String taskId) {
        return historyDao.findByTaskId(taskId);
    }

    /**
     * 登录时将未登录会话的历史记录归属到真实用户
     * 根据 sessionId 找到所有 userId IS NULL 的记录，更新为真实用户ID
     * 适用于：用户未登录时试用一整套工具，觉得满意后注册登录
     */
    public int mergeGuestHistoryToUser(HttpSession session, int userId) {
        String sessionId = session.getId();
        return historyDao.updateUserIdBySessionId(userId, sessionId);
    }

}
