package com.manju.platform.service;

import com.manju.platform.common.Constants;
import com.manju.platform.common.PromptUtils;
import com.manju.platform.dto.SceneGenerateRequest;
import com.manju.platform.dto.SceneGenerateResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class SceneService {
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private AIService aiService;

    public SceneGenerateResponse generateScene(int userId, SceneGenerateRequest req) {
        return paymentService.processPayment(
                userId,
                Constants.TOOL_SCENE_GENERATE,
                Constants.POINTS_SCENE_GENERATE,
                () -> {
                    // 拼接风格声明到prompt
                    String prompt = PromptUtils.buildPromptWithStyle(req.getScenePrompt(), req.getStyleDeclaration());
                    String imageUrl = aiService.generateImage(
                            prompt,
                            Collections.emptyList()
                    );
                    SceneGenerateResponse response = new SceneGenerateResponse();
                    response.setImageUrl(imageUrl);
                    return response;
                }
        );
    }
}
