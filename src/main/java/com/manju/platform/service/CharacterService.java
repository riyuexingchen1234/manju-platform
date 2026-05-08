package com.manju.platform.service;

import com.manju.platform.common.Constants;
import com.manju.platform.dto.CharacterGenerateRequest;
import com.manju.platform.dto.CharacterGenerateResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CharacterService {
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private AIService aiService;

    public CharacterGenerateResponse generateCharacter(int userId, CharacterGenerateRequest req) {
        return paymentService.processPayment(
                userId,
                Constants.TOOL_CHARACTER_GENERATE,
                Constants.POINTS_CHARACTER_GENERATE,
                () -> {
                    // 拼接风格声明到prompt
                    String prompt = buildPromptWithStyle(req.getCharacterPrompt(), req.getStyleDeclaration());
                    String imageUrl = aiService.generateImage(
                            prompt,
                            Collections.emptyList()
                    );
                    CharacterGenerateResponse response = new CharacterGenerateResponse();
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
