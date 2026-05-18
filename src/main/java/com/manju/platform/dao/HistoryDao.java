package com.manju.platform.dao;

import com.manju.platform.entity.UserHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 历史记录数据访问层
 * 对应表：user_history
 */
@Repository
public class HistoryDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<UserHistory> historyRowMapper = new RowMapper<UserHistory>() {
        @Override
        public UserHistory mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserHistory h = new UserHistory();
            h.setId(rs.getInt("id"));
            Object userIdObj = rs.getObject("user_id");
            h.setUserId(userIdObj != null ? (Integer) userIdObj : null);
            h.setTool(rs.getString("tool"));
            h.setInputPreview(rs.getString("input_preview"));
            h.setResultText(rs.getString("result_text"));
            h.setResultUrl(rs.getString("result_url"));
            h.setStatus(rs.getString("status"));
            h.setTaskId(rs.getString("task_id"));
            h.setSessionId(rs.getString("session_id"));
            h.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            return h;
        }
    };

    public int insert(UserHistory history) {
        String sql = "INSERT INTO user_history (user_id, tool, input_preview, result_text, result_url, status, task_id, session_id, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        return jdbcTemplate.update(sql,
                history.getUserId(),
                history.getTool(),
                history.getInputPreview(),
                history.getResultText(),
                history.getResultUrl(),
                history.getStatus(),
                history.getTaskId(),
                history.getSessionId()
        );
    }

    public int updateById(UserHistory history) {
        String sql = "UPDATE user_history SET result_text = ?, result_url = ?, status = ? WHERE id = ?";
        return jdbcTemplate.update(sql,
                history.getResultText(),
                history.getResultUrl(),
                history.getStatus(),
                history.getId()
        );
    }

    public UserHistory findByTaskId(String taskId) {
        String sql = "SELECT id, user_id, tool, input_preview, result_text, result_url, status, task_id, session_id, created_at " +
                "FROM user_history WHERE task_id = ?";
        List<UserHistory> list = jdbcTemplate.query(sql, historyRowMapper, taskId);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<UserHistory> findRecentByUserId(int userId, int limit) {
        String sql = "SELECT id, user_id, tool, input_preview, result_text, result_url, status, task_id, session_id, created_at " +
                "FROM user_history WHERE user_id = ? ORDER BY created_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, historyRowMapper, userId, limit);
    }

    public List<UserHistory> findByUserId(int userId, int offset, int size) {
        String sql = "SELECT id, user_id, tool, input_preview, result_text, result_url, status, task_id, session_id, created_at " +
                "FROM user_history WHERE user_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, historyRowMapper, userId, size, offset);
    }

    public int countByUserId(int userId) {
        String sql = "SELECT COUNT(*) FROM user_history WHERE user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null ? count : 0;
    }

    /**
     * 将指定会话的所有未归属记录归属到真实用户
     * 用于登录时合并未登录试用记录
     */
    public int updateUserIdBySessionId(int userId, String sessionId) {
        String sql = "UPDATE user_history SET user_id = ? WHERE session_id = ? AND user_id IS NULL";
        return jdbcTemplate.update(sql, userId, sessionId);
    }
}
