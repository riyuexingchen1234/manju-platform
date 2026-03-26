package com.manju.platform.dto;
import lombok.Data;
//视频生成响应
@Data
public class VideoGenerateResponse {
    private String taskId;       // 任务ID
    private String status;       // 任务状态（PROCESSING/SUCCESS/FAIL）
    private String videoUrl;     // 如果成功，这里有视频URL
    private String errorMsg;     // 如果失败，这里有错误信息
}
