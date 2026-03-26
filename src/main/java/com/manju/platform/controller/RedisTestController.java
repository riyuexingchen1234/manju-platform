//package com.manju.platform.controller;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/test")
//public class RedisTestController {
//
//    @Autowired
//    private StringRedisTemplate redisTemplate;
//
//    @GetMapping("/redis")
//    public String testRedis() {
//        redisTemplate.opsForValue().set("testKey", "Hello Redis");
//        return redisTemplate.opsForValue().get("testKey");
//    }
//}