package com.manju.platform.dao;

import com.manju.platform.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用户数据访问层，封装对 user 表的所有数据库操作（支持事务），无业务逻辑
 */
@Repository
public class UserDao {
    private static final Logger logger = LoggerFactory.getLogger(UserDao.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 定义一个RowMapper，用于将查询结果的行转换为User对象
    private final RowMapper<User> userRowMapper = new RowMapper<User>() {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setId(rs.getInt("id"));
            user.setUsername(rs.getString("username"));
            user.setPassword(rs.getString("password"));
            user.setPoints(rs.getInt("points"));
            user.setCreateTime(rs.getObject("create_time", LocalDateTime.class));
            user.setVersion(rs.getInt("version"));
            return user;
        }
    };

    public User findById(int userId) {
        String sql = "SELECT id, username, password, points, create_time, version FROM user WHERE id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, userRowMapper, userId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public User findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            logger.debug("findByUsername: username is null or empty");
            return null;
        }
        String sql = "SELECT id, username, password, points, create_time, version FROM user WHERE username = ?";
        logger.debug("findByUsername: executing query for username = [{}]", username);
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, username);
            logger.debug("findByUsername: query result = {}", user);
            return user;
        } catch (EmptyResultDataAccessException e) {
            logger.debug("findByUsername: user not found for [{}]", username);
            return null;
        }
    }

    public boolean updatePoints(int userId, int newPoints) {
        String sql = "UPDATE user SET points = ? WHERE id = ?";
        int rows = jdbcTemplate.update(sql, newPoints, userId);
        return rows > 0;
    }

    public boolean updatePointsWithVersion(int userId, int newPoints, int currentVersion) {
        String sql = "UPDATE user SET points = ?, version = version + 1 WHERE id = ? AND version = ?";
        int rows = jdbcTemplate.update(sql, newPoints, userId, currentVersion);
        return rows > 0;
    }

    /**
     * 退还积分（乐观锁）
     * 用于AI调用失败后回退预扣的积分
     */
    public boolean refundPoints(int userId, int pointsToRefund) {
        String sql = "UPDATE user SET points = points + ?, version = version + 1 WHERE id = ?";
        int rows = jdbcTemplate.update(sql, pointsToRefund, userId);
        return rows > 0;
    }

    public int insert(User user) {
        String sql = "INSERT INTO user (username, password, points, create_time, version) VALUES (?, ?, ?, ?, ?)";
        return jdbcTemplate.update(sql,
                user.getUsername(),
                user.getPassword(),
                user.getPoints(),
                user.getCreateTime(),
                user.getVersion()
        );
    }
}
