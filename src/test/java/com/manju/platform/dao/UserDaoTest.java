package com.manju.platform.dao;

import com.manju.platform.dao.UserDao;
import com.manju.platform.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class UserDaoTest {
    @Autowired
    private UserDao userDao;

    @Test
    void testFindByUsername(){
        String username = "test1";
        System.out.println("查询用户名：" + username);
        User user = userDao.findByUsername(username);
        System.out.println("查询结果："+ user);
        assertNotNull(user);
    }
}
