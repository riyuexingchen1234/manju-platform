package com.manju.platform.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UsageLog {
    private int id;
    private int userId;
    private String toolName;
    private LocalDateTime useDate;
    private int isFree;
    private int pointsDeduct;
    private int callStatus;
    private String failReason;
}
