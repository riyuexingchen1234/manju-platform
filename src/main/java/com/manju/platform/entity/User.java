package com.manju.platform.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User implements java.io.Serializable{
    private static final long serivalVersionUID = 1L;
    private int id;
    private String username;
    private String password;
    private int points;
    private LocalDateTime createTime;
    private int version;
}



