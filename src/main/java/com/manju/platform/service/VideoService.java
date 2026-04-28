package com.manju.platform.service;

import com.manju.platform.common.Constants;
import com.manju.platform.dto.VideoGenerateRequest;
import com.manju.platform.dto.VideoGenerateResponse;
import com.manju.platform.functional.AICallable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class VideoService {
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private AIService aiService;

    public VideoGenerateResponse generateVideo(int userId,VideoGenerateRequest req) {
        Object result = paymentService.processPayment(
                userId,
                Constants.TOOL_VIDEO_GENERATE,
                Constants.POINTS_VIDEO_GENERATE,
                () ->{
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
        );
        return (VideoGenerateResponse) result;
    }
}
