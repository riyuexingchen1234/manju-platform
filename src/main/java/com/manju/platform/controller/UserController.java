package com.manju.platform.controller;

import com.manju.platform.common.Result;
import com.manju.platform.dao.UserDao;
import com.manju.platform.entity.User;
import com.manju.platform.service.HistoryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserDao userDao;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private HistoryService historyService;

    @PostMapping("/login")
    public Result login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session) {
        logger.debug("收到登录请求：username={}", username);
        User user = userDao.findByUsername(username);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            session.setAttribute("userId", user.getId());
            session.setAttribute("user", user);
            session.removeAttribute("trialDateMap");
            int mergedCount = historyService.mergeGuestHistoryToUser(session, user.getId());
            if (mergedCount > 0) {
                logger.debug("已合并 {} 条未登录试用历史到用户 {}", mergedCount, user.getId());
            }
            return Result.success("登录成功", user);
        }
        return Result.fail("用户名或密码错误");
    }

    @GetMapping("/{id}/points")
    public Result getPoints(@PathVariable int id) {
        User user = userDao.findById(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        return Result.success("查询成功", user.getPoints());
    }

    @PostMapping("/logout")
    public Result logout(HttpSession session) {
        session.invalidate();
        return Result.success("退出登录成功");
    }

    @PostMapping("/register")
    public Result register(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");

        if (username == null || username.trim().isEmpty()
                || password == null || password.length() < 6) {
            return Result.fail("用户名不能为空且密码至少6位");
        }

        User exist = userDao.findByUsername(username);
        if (exist != null) {
            return Result.fail("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setPoints(100);
        user.setVersion(0);
        user.setCreateTime(LocalDateTime.now());

        int rows = userDao.insert(user);
        if (rows > 0) {
            return Result.success("注册成功", user);
        } else {
            return Result.fail("注册失败，请稍后重试");
        }
    }
}
