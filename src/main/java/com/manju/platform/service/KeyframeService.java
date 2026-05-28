package com.manju.platform.service;

import com.manju.platform.common.Constants;
import com.manju.platform.common.PromptUtils;
import com.manju.platform.dto.KeyframeGenerateRequest;
import com.manju.platform.dto.KeyframeGenerateResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class KeyframeService {
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private AIService aiService;

    public KeyframeGenerateResponse generateKeyframe(int userId, KeyframeGenerateRequest req) {
        return paymentService.processPayment(
                userId,
                Constants.TOOL_KEYFRAME_GENERATE,
                Constants.POINTS_KEYFRAME_GENERATE,
                () -> {
                    // 构建 imageUrls：先放所有角色图，再放场景图
                    List<String> imageUrls = new ArrayList<>();
                    if (req.getCharacterImageUrls() != null) {
                        imageUrls.addAll(req.getCharacterImageUrls());
                    }
                    if (req.getSceneImageUrl() != null) {
                        imageUrls.add(req.getSceneImageUrl());
                    }

                    // 构建 prompt
                    int charCount = req.getCharacterImageUrls() != null ? req.getCharacterImageUrls().size() : 0;
                    String prompt = PromptUtils.buildKeyframePrompt(req.getStoryboardDescription(), charCount);

                    String imageUrl = aiService.generateImage(prompt, imageUrls);
                    KeyframeGenerateResponse response = new KeyframeGenerateResponse();
                    response.setImageUrl(imageUrl);
                    return response;
                }
        );
    }
}
