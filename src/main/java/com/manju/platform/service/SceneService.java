package com.manju.platform.service;

import com.manju.platform.common.Constants;
import com.manju.platform.dto.SceneGenerateRequest;
import com.manju.platform.dto.SceneGenerateResponse;
import com.manju.platform.functional.AICallable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class SceneService {
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private AIService aiService;

    public SceneGenerateResponse generateScene(int userId,SceneGenerateRequest req){
        Object result = paymentService.processPayment(
                userId,
                Constants.TOOL_SCENE_GENERATE,
                Constants.POINTS_SCENE_GENERATE,
                () -> {
                    // 调用 AI 生成图片（文生图，无参考图）
                    String imageUrl = aiService.generateImageFromMultimodal(
                            req.getScenePrompt(),
                            Collections.emptyList()
                    );
                    // 封装响应
                    SceneGenerateResponse response = new SceneGenerateResponse();
                    response.setImageUrl(imageUrl);
                    return response;
                }
        );
        return (SceneGenerateResponse) result;
    }
}
