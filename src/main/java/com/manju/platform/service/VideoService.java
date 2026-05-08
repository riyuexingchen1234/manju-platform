package com.manju.platform.service;

import com.manju.platform.dto.VideoGenerateRequest;
import com.manju.platform.dto.VideoGenerateResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class VideoService {
    @Autowired
    private AIService aiService;

    /**
     * 生成视频（异步任务）
     * 视频服务采用延迟扣费模式：提交时只创建任务，不扣费；
     * 等前端轮询到任务成功后再进行扣费
     */
    public VideoGenerateResponse generateVideo(VideoGenerateRequest req) {
        // 调用阿里云AI创建视频生成任务
        Map<String, String> taskInfo = aiService.createVideoGenerationTask(
                req.getKeyframeImageUrl(),
                req.getDescription()
        );
        // 构建响应
        VideoGenerateResponse response = new VideoGenerateResponse();
        response.setTaskId(taskInfo.get("taskId"));
        response.setStatus(taskInfo.get("status"));
        return response;
    }
}
