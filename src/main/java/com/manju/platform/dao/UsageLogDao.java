package com.manju.platform.dao;

import com.manju.platform.entity.UsageLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;

/**
 * 工具使用记录DAO，仅操作Usage_log日志表，无业务逻辑
 */
@Repository
public class UsageLogDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    /**
     * 插入使用记录，并返回自动生成的主键ID
     * @throws // DuplicateKeyException 如果唯一约束冲突（今日已免费使用）
     * 返回 true 表示插入成功，false 表示唯一约束冲突（今日已免费）
     */
    public int insertAndReturnId(UsageLog log) {
        String sql = "INSERT INTO usage_log (user_id,tool_name,is_free,points_deduct,call_status,fail_reason) VALUES (?,?,?,?,?,?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, log.getUserId());
            ps.setString(2, log.getToolName());
            ps.setInt(3, log.getIsFree());
            ps.setInt(4, log.getPointsDeduct());
            ps.setInt(5, log.getCallStatus());
            ps.setString(6, log.getFailReason());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().intValue();
    }
    /**
     * 更新日志状态（用于免费流程中 AI 成功后更新 call_status 和 fail_reason）
     */
    public void updateLogStatus(int id,int callStatus, String failReason){
        String sql = "UPDATE usage_log SET call_status = ?, fail_reason = ? WHERE id = ?";
        jdbcTemplate.update(sql,callStatus,failReason, id);
    }
}

