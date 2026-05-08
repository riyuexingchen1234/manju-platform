package com.manju.platform.service;

import com.manju.platform.common.Constants;
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
                    String prompt = buildPromptWithStyle(req.getScenePrompt(), req.getStyleDeclaration());
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

    /**
     * 将全局风格声明拼接到用户prompt末尾
     * @param userPrompt 用户提供的原始prompt
     * @param styleDeclaration 全局风格声明
     * @return 拼接后的完整prompt
     */
    private String buildPromptWithStyle(String userPrompt, String styleDeclaration) {
        if (userPrompt == null) {
            userPrompt = "";
        }
        if (styleDeclaration != null && !styleDeclaration.trim().isEmpty()) {
            return userPrompt + "\n\n【全局风格声明】" + styleDeclaration;
        }
        return userPrompt;
    }
}
