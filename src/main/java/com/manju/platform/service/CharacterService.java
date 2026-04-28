package com.manju.platform.service;

import com.manju.platform.common.Constants;
import com.manju.platform.dto.CharacterGenerateRequest;
import com.manju.platform.dto.CharacterGenerateResponse;
import com.manju.platform.functional.AICallable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CharacterService {
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private AIService aiService;

    public CharacterGenerateResponse generateCharacter(int userId,CharacterGenerateRequest req){
        Object result = paymentService.processPayment(
                userId,
                Constants.TOOL_CHARACTER_GENERATE,
                Constants.POINTS_CHARACTER_GENERATE,
                () -> {
                    // 免费流程：调用 AI 生成图片（文生图，无参考图）
                    String imageUrl = aiService.generateImageFromMultimodal(
                            req.getCharacterPrompt(),
                            Collections.emptyList()
                    );
                    // 封装响应
                    CharacterGenerateResponse response = new CharacterGenerateResponse();
                    response.setImageUrl(imageUrl);
                    return response;
                }
        );
        return (CharacterGenerateResponse) result;
    }
}
