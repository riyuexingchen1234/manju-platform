package com.manju.platform.dao;

import com.manju.platform.entity.UsageLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 工具使用记录DAO，仅操作Usage_log日志表，无业务逻辑
 */
@Repository
public class UsageLogDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 查询用户今日是否已免费使用某工具
    public boolean isFreeUsedToday(int userId, String toolName) {
        String sql = "SELECT COUNT(*) FROM usage_log WHERE user_id=? AND tool_name = ? " +
                "AND is_free= 1 AND DATE(use_date) = CURDATE()";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, toolName);
        return count != null && count > 0;
    }

    // 插入使用记录
    public int insert(UsageLog log) {
        String sql = "INSERT INTO usage_log (user_id,tool_name,is_free,points_deduct,call_status,fail_reason) VALUES (?,?,?,?,?,?)";
        return jdbcTemplate.update(sql,
                log.getUserId(),
                log.getToolName(),
                log.getIsFree(),
                log.getPointsDeduct(),
                log.getCallStatus(),
                log.getFailReason());
    }


}

