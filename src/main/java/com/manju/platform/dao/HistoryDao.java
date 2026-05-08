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
            h.setUserId(rs.getInt("user_id"));
            h.setToolType(rs.getString("tool_type"));
            h.setInputPreview(rs.getString("input_preview"));
            h.setResultType(rs.getString("result_type"));
            h.setResultText(rs.getString("result_text"));
            h.setResultUrl(rs.getString("result_url"));
            h.setStatus(rs.getString("status"));
            h.setTaskId(rs.getString("task_id"));
            h.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            return h;
        }
    };

    public int insert(UserHistory history) {
        String sql = "INSERT INTO user_history (user_id, tool_type, input_preview, result_type, result_text, result_url, status, task_id, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        return jdbcTemplate.update(sql,
                history.getUserId(),
                history.getToolType(),
                history.getInputPreview(),
                history.getResultType(),
                history.getResultText(),
                history.getResultUrl(),
                history.getStatus(),
                history.getTaskId()
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
        String sql = "SELECT id, user_id, tool_type, input_preview, result_type, result_text, result_url, status, task_id, created_at " +
                "FROM user_history WHERE task_id = ?";
        List<UserHistory> list = jdbcTemplate.query(sql, historyRowMapper, taskId);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<UserHistory> findRecentByUserId(int userId, int limit) {
        String sql = "SELECT id, user_id, tool_type, input_preview, result_type, result_text, result_url, status, task_id, created_at " +
                "FROM user_history WHERE user_id = ? ORDER BY created_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, historyRowMapper, userId, limit);
    }

    public List<UserHistory> findByUserId(int userId, int offset, int size) {
        String sql = "SELECT id, user_id, tool_type, input_preview, result_type, result_text, result_url, status, task_id, created_at " +
                "FROM user_history WHERE user_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, historyRowMapper, userId, size, offset);
    }

    public int countByUserId(int userId) {
        String sql = "SELECT COUNT(*) FROM user_history WHERE user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null ? count : 0;
    }
}
