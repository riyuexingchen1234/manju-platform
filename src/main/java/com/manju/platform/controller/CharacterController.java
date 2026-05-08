package com.manju.platform.controller;

import com.manju.platform.common.Constants;
import com.manju.platform.common.Result;
import com.manju.platform.dto.CharacterGenerateRequest;
import com.manju.platform.dto.CharacterGenerateResponse;
import com.manju.platform.service.AIService;
import com.manju.platform.service.CharacterService;
import com.manju.platform.service.GuestTrialService;
import com.manju.platform.service.HistoryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/api/character")
public class CharacterController {
    @Autowired
    private CharacterService characterService;
    @Autowired
    private AIService aiService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private GuestTrialService guestTrialService;

    // 提取角色描述前50字作为 input_preview
    private String extractInputPreview(String prompt) {
        if (prompt != null) {
            return prompt.length() > 50 ? prompt.substring(0, 50) + "..." : prompt;
        }
        return "";
    }

    @PostMapping("/generate")
    public Result generateCharacter(@RequestBody CharacterGenerateRequest request, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");

        // 构建带风格声明的prompt（登录用户和试用用户都需要）
        String prompt = buildPromptWithStyle(request.getCharacterPrompt(), request.getStyleDeclaration());

        // 未登录用户试用
        if (userId == null) {
            String imageUrl = guestTrialService.execute(session,
                    Constants.TOOL_CHARACTER_GENERATE, Constants.DISPLAY_CHARACTER_GENERATE,
                    () -> aiService.generateImage(
                            prompt,
                            Collections.emptyList()
                    ));
            historyService.save(null, session, Constants.TOOL_CHARACTER_GENERATE,
                    extractInputPreview(request.getCharacterPrompt()),
                    "image_url", null, imageUrl, "success", null);
            return Result.success("试用成功", imageUrl);
        }

        // 已登录用户
        CharacterGenerateResponse response = characterService.generateCharacter(userId, request);
        historyService.save(userId, session, Constants.TOOL_CHARACTER_GENERATE,
                extractInputPreview(request.getCharacterPrompt()),
                "image_url", null, response.getImageUrl(), "success", null);
        return Result.success("角色生成成功", response);
    }

    /**
     * 将全局风格声明拼接到用户prompt末尾
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
