package com.manju.platform.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * implements java.io.Serializable
 * 序列化接口，表示这个类的对象可以被转换成字节流，用于：
 * 把对象存到 Redis 缓存;把对象通过网络传输;把对象序列化到文件
 * private static final long serialVersionUID = 1L;
 * 序列化版本号，是序列化机制的核心：
 * 当序列化一个对象时，会把这个版本号一起存起来
 * 反序列化时，会检查对象的版本号和类的版本号是否一致
 * 如果不一致，会抛出InvalidClassException异常
 * 写1L是最简单、最常用的写法，只要修改了类的字段（比如加了个email字段），就把这个版本号改成2L，避免反序列化失败。
 */
@Data
public class User implements java.io.Serializable{
    private static final long serialVersionUID = 1L;
    private int id;
    private String username;
    private String password;
    private int points;
    private LocalDateTime createTime;
    private int version;
}



