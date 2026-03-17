//package com.manju.platform.service;
//import com.manju.platform.dao.UserDao;
//import com.manju.platform.entity.User;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//// SpringBootTest注解会启动Spring应用上下文，加载所有配置和Bean，
//// 这样就可以在测试中自动注入 ToolService 等组件。
//
//import javax.swing.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@SpringBootTest
//public class ToolServiceTest {
//    @Autowired
//    private ToolService toolService;
//    @Autowired
//    private UserDao userDao;
//
//    @BeforeEach
//    void setUp(){
//        // 每次测试前重置数据（确保积分是100和5）
//        userDao.updatePoints(1,100);
//        userDao.updatePoints(2,5);
//    }
//    @Test
//    void testUserNotFound(){
//        String result = toolService.useTool(999,"剧本生成");
//        assertEquals("用户不存在",result);
//    }
//    //assertEquals 是单元测试框架中最常用的断言方法，核心作用是验证「预期值」和「实际值」是否相等，
//    // 如果不相等则抛出断言失败异常，标记测试用例不通过。
//    //接收两个核心参数：预期值(expected) 和 实际值(actual)。
//    //执行时会对比这两个值：
//    //相等 → 测试通过，无任何提示；
//    //不相等 → 抛出 AssertionError，并在异常信息中显示预期值和实际值，方便定位问题。
//
//    @Test
//    void testFirstCallFree(){
//        String result = toolService.useTool(1,"剧本生成");
//        assertEquals("免费调用成功",result);
//        // 验证积分没变
//        User user = userDao.findById(1);
//        assertEquals(100,user.getPoints());
//    }
//
//    @Test
//    void testSecondCallPay(){
//        // 第一次免费
//        toolService.useTool(1,"剧本生成");
//        // 第二次付费
//        String result = toolService.useTool(1,"剧本生成");
//        assertTrue(result.startsWith("付费调用成功"));
//        User user = userDao.findById(1);
//        assertEquals(90,user.getPoints());  // 100-10
//    }
//    //assertTrue 是单元测试框架中最基础的断言方法，核心作用是验证某个条件是否为真（True），
//    // 如果条件为假（False），则会抛出断言失败异常，标记测试用例不通过。
//    //可以把它想象成测试中的 “判断题”：你预期某个操作的结果应该是 “对的”，
//    // 用 assertTrue 去检查，如果实际结果是 “错的”，测试就会报错，提醒你代码可能有问题。
//    @Test
//    void testInsufficientPoints(){
//        // 用户2积分5，第一次免费
//        toolService.useTool(2, "剧本生成");
//        // 第二次付费，积分不足
//        String result = toolService.useTool(2, "剧本生成");
//        assertTrue(result.contains("积分不足"));
//        User user = userDao.findById(2);
//        assertEquals(5, user.getPoints()); // 积分不变
//    }
//}
