package com.manju.platform.dto;
import lombok.Data;
import java.util.List;

// 关键帧生成请求

@Data
public class KeyframeGenerateRequest {
    private String storyboardDescription;       // 分镜描述
    private List<String> characterImageUrls;   // 角色图URL列表（支持多角色）
    private String sceneImageUrl;               // 场景图URL
}
