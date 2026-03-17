package com.manju.platform.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {
    private int id;
    private String username;
    private String password;
    private int points;
    private LocalDateTime createTime;
}



