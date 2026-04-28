package com.manju.platform.dto;
import lombok.Data;
//  视频生成请求
@Data
public class VideoGenerateRequest {
    private String keyframeImageUrl;
    private String description;
}
