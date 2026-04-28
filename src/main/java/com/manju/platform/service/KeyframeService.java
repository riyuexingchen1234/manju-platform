package com.manju.platform.service;


import com.manju.platform.common.Constants;
import com.manju.platform.dto.KeyframeGenerateRequest;
import com.manju.platform.dto.KeyframeGenerateResponse;
import com.manju.platform.functional.AICallable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class KeyframeService {
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private AIService aiService;

    public KeyframeGenerateResponse generateKeyframe(int userId,KeyframeGenerateRequest req){
        Object result = paymentService.processPayment(
                userId,
                Constants.TOOL_KEYFRAME_GENERATE,
                Constants.POINTS_KEYFRAME_GENERATE,
                () ->{
                    // 构造融合提示词（将分镜描述嵌入）
                    String prompt = String.format(
                            "使用第一张图作为角色形象，第二张图作为背景场景。请将角色自然地融入场景中，执行动作：%s。保持角色和场景的真实性，生成一张融合后的新图片。",
                            req.getStoryboardDescription()
                    );

                    // 参考图列表：角色图、场景图
                    List<String> imageUrls = Arrays.asList(req.getCharacterImageUrl(), req.getSceneImageUrl());

                    // 调用 AI 生成关键帧图
                    String imageUrl = aiService.generateImageFromMultimodal(prompt, imageUrls);
                    // 封装响应
                    KeyframeGenerateResponse response = new KeyframeGenerateResponse();
                    response.setImageUrl(imageUrl);
                    return response;
                }
        );
        return (KeyframeGenerateResponse) result;
    }
}
