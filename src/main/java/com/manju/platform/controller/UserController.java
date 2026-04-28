package com.manju.platform.controller;

import com.manju.platform.common.Result;
import com.manju.platform.dao.UserDao;
import com.manju.platform.entity.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 * 作用：处理所有与用户相关的HTTP请求（登录、查询积分等）
 * 接口前缀：所有接口的URL都以 /api/user 开头
 */
// Spring注解：标记该类为REST控制器，所有方法返回的数据会自动转换为JSON格式（而非跳转页面）
@RestController
// Spring注解：定义该类所有接口的URL统一前缀
@RequestMapping("/api/user")
public class UserController {
    // Spring注解：自动装配UserDao对象，无需手动new，直接使用即可
    @Autowired
    private UserDao userDao;

    // 登录接口（简化版）
    // 验证用户名和密码，验证成功后将用户信息存入Session（维持登录状态）
    @PostMapping("/login")   // Spring注解：映射POST请求到该方法，路径为 /api/user/login
    // @RequestParam：从请求的URL参数或表单数据中获取值
    public Result login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session) {
        // 打印登录请求日志（方便开发调试，查看是否收到正确的参数）
        System.out.println("收到登陆请求：username=" + username + "，password=" + password);
        // 1. 根据用户名查询数据库中的用户信息
        User user = userDao.findByUsername(username);
        // 2. 验证用户是否存在且密码是否正确
        if (user != null && user.getPassword().equals(password)) {
        // 3. 验证成功：将用户ID和用户对象存入Session（后续接口可通过Session识别当前登录用户）
            session.setAttribute("userId",user.getId());
            session.setAttribute("user",user);
        // 4. 清除未登录时的试用记录（登录后使用正式积分，不再需要试用数据）
            session.removeAttribute("trialMap");
        // 5. 返回登录成功结果，并将用户信息返回给前端
            return Result.success("登陆成功", user);
        }
        // 6. 验证失败：返回用户名或密码错误的提示
        return Result.fail("用户名或密码错误");
    }

    /**
     * 查询用户积分接口
     * 请求方式：GET
     * 请求路径：/api/user/{id}/points
     * 功能：根据用户ID查询该用户的当前积分余额
     */
    // Spring注解：映射GET请求到该方法，{id} 是路径变量
    @GetMapping("/{id}/points")
    // @PathVariable：从URL路径中提取 {id} 的值赋给参数 id
    public Result getPoints(@PathVariable int id) {
        // 1. 根据用户ID查询数据库中的用户信息
        User user = userDao.findById(id);
        // 2. 判断用户是否存在
        if (user == null) {
            return Result.fail("用户不存在");
        }
        // 3. 用户存在：返回查询成功结果，并将积分余额返回给前端
        return Result.success("查询成功", user.getPoints());
    }

    // 登出接口
    @PostMapping("/logout")
    public Result logout(HttpSession session){
        session.invalidate();
        return Result.success("退出登录成功");
    }

}
