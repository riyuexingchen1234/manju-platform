package com.manju.platform.dao;

import com.manju.platform.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
// 依赖注入（DI）注解，作用是自动从 Spring 容器中找到匹配的对象，注入到当前类的属性 / 方法 / 构造器中，无需手动 new 对象，实现解耦。
//通俗理解：你想要用 JdbcTemplate 对象，不用自己创建，Spring 会 “自动送过来”。
//注入规则：默认按类型匹配（如 JdbcTemplate 类型），如果有多个同类型 Bean，需配合 @Qualifier 指定名称。
import org.springframework.jdbc.core.JdbcTemplate;
//JdbcTemplate 是 Spring 封装的 JDBC 工具类，替代了手动写 Connection/PreparedStatement/ResultSet 的繁琐代码，解决了原生 JDBC 的资源关闭、异常处理等问题，核心能力：
//简化数据库增删改查（CRUD）操作
//自动管理数据库连接的创建和释放
//处理 JDBC 异常并转换为 Spring 统一的异常体系
import org.springframework.jdbc.core.RowMapper;
// RowMapper 是一个接口，用来把数据库的一行数据转换成Java对象。实现了它的 mapRow 方法，从 ResultSet 中取出字段，设置到 User 对象里。
import org.springframework.stereotype.Repository; // 数据访问层组件，用于表示负责与数据库交互、处理数据存取的类，相当于 DAO

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * 用户数据访问层，封装对 user 表的所有数据库操作（支持事务），无业务逻辑
 */

@Repository
public class UserDao {

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
            // 注意：数据库字段是create_time，对应实体类的createTime
            user.setCreateTime(rs.getObject("create_time", LocalDateTime.class));
            user.setVersion(rs.getInt("version"));
            return user;
        }
    };

    public User findById(int userId) {
        String sql = "SELECT id, username, password, points, create_time, version FROM user WHERE id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, userRowMapper, userId);
        } catch (Exception e) {
            // 查询不到记录会抛异常，返回null
            return null;
        }
    }

    public User findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            System.out.println("findByUsername:username is null or empty");
            return null;
        }
        String sql = "SELECT id, username, password,points,create_time, version FROM user WHERE username = ?";
        System.out.println("findByUsername: executing query for username = [" + username + "]");
        try {
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, username);
            System.out.println("findByUsername: query result = " + user);
            return user;
        } catch (Exception e) {
            System.out.println("findByUsername: exception occurred:");
            e.printStackTrace(); // 打印完整异常
            return null;
        }
    }
    // 无锁更新积分
    public boolean updatePoints(int userId, int newPoints) {
        String sql = "UPDATE user SET points = ? WHERE id = ?";
        int rows = jdbcTemplate.update(sql, newPoints, userId);
        return rows > 0;
    }

    /**
     * 基于乐观锁更新用户积分
     * 作用：解决并发场景下多个请求同时更新同一用户积分导致的“积分丢失”问题
     * 乐观锁原理：
     * - 每次读取用户数据时，同时读取当前的版本号（version）
     * - 更新时，在WHERE条件中带上版本号，只有数据库里的版本号与传入的一致时才更新
     * - 更新成功后，版本号自动加1
     * - 如果有其他线程先更新了数据，版本号会变化，当前更新就会失败（返回false）
     *
     * @param userId         要更新积分的用户ID
     * @param newPoints      要设置的新积分值（计算后的最终积分，如：原积分 - 10）
     * @param currentVersion 更新前读取到的当前版本号（用于乐观锁校验）
     * @return true表示更新成功；false表示更新失败（版本号不匹配，存在并发冲突）
     */
    public boolean updatePointsWithVersion(int userId, int newPoints, int currentVersion) {
        String sql = "UPDATE user SET points = ?, version = version + 1 WHERE id = ? AND version = ?";
        int rows = jdbcTemplate.update(sql, newPoints, userId, currentVersion);
        // 判断更新是否成功：
        // rows > 0 表示受影响行数大于0，即更新成功（版本号匹配）
        // rows = 0 表示没有更新到数据，即版本号不匹配（有其他线程先更新了）
        return rows > 0;
    }

}




