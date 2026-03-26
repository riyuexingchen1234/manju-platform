package com.manju.platform.dto;
import lombok.Data;

//  关键帧生成请求

@Data
public class KeyframeGenerateRequest {
    private Integer userId;
    private String storyboardDescription;   // 分镜描述
    private String characterImageUrl;       // 角色图URL
    private String sceneImageUrl;           // 场景图URL
}
