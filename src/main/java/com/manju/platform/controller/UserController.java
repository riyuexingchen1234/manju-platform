package com.manju.platform.controller;

import com.manju.platform.common.Result;
import com.manju.platform.dao.UserDao;
import com.manju.platform.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 创建控制器，提供登录和查询积分接口。
 */
@RestController     // 表示这个类处理HTTP请求，返回的数据会自动转成JSON。
@RequestMapping("/api/user")    // 所有接口的URL前缀。
public class UserController {
    @Autowired
    private UserDao userDao;

    // 登录接口（简化版，根据用户名和密码验证）
    @PostMapping("/login")      // @PostMapping:对应POST请求,@RequestParam：从请求参数中取值。
    public Result login(@RequestParam String username, @RequestParam String password) {
        System.out.println("收到登陆请求：username=" + username + "，password=" + password);
        User user = userDao.findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            return Result.success("登陆成功", user);
        }
        return Result.fail("用户名或密码错误");
    }

    // 查询积分
    @GetMapping("/{id}/points")     // @GetMapping:对应GET请求，@PathVariable：从URL路径中取值。
    public Result getPoints(@PathVariable int id) {
        User user = userDao.findById(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        return Result.success("查询成功", user.getPoints());
    }

}
