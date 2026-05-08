package com.manju.platform.service;

import com.manju.platform.common.Constants;
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
                    String prompt;
                    if (charCount == 0) {
                        // 无角色图时，只用场景图
                        prompt = String.format(
                                "请根据以下场景参考图生成关键帧图片：%s。%s",
                                req.getSceneImageUrl() != null ? "图1是场景背景参考图" : "",
                                req.getStoryboardDescription()
                        );
                    } else if (charCount == 1) {
                        // 单角色
                        prompt = String.format(
                                "请根据以下参考图和描述生成关键帧图片。图1是角色形象参考图，图2是场景背景参考图。请将角色自然地融入场景中，%s",
                                req.getStoryboardDescription()
                        );
                    } else {
                        // 多角色
                        prompt = String.format(
                                "请根据以下参考图和描述生成关键帧图片。图1到图%d是角色形象参考图，图%d是场景背景参考图。请将角色自然地融入场景中，%s",
                                charCount,
                                charCount + 1,
                                req.getStoryboardDescription()
                        );
                    }

                    String imageUrl = aiService.generateImage(prompt, imageUrls);
                    KeyframeGenerateResponse response = new KeyframeGenerateResponse();
                    response.setImageUrl(imageUrl);
                    return response;
                }
        );
    }
}
