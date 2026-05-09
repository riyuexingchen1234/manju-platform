package com.manju.platform.service;

import com.manju.platform.common.Constants;
import com.manju.platform.dto.VideoGenerateRequest;
import com.manju.platform.dto.VideoGenerateResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class VideoService {
    @Autowired
    private AIService aiService;
    @Autowired
    private PaymentService paymentService;

    /**
     * 生成视频（异步任务）
     * 统一使用processPayment：先扣后调，失败退还
     * createVideoGenerationTask如果抛异常则退还积分，正常返回taskId则计费
     */
    public VideoGenerateResponse generateVideo(int userId, VideoGenerateRequest req) {
        return paymentService.processPayment(
                userId,
                Constants.TOOL_VIDEO_GENERATE,
                Constants.POINTS_VIDEO_GENERATE,
                () -> {
                    Map<String, String> taskInfo = aiService.createVideoGenerationTask(
                            req.getKeyframeImageUrl(),
                            req.getDescription()
                    );
                    VideoGenerateResponse response = new VideoGenerateResponse();
                    response.setTaskId(taskInfo.get("taskId"));
                    response.setStatus(taskInfo.get("status"));
                    return response;
                }
        );
    }
}
