package com.manju.platform.controller;

import com.manju.platform.common.Constants;
import com.manju.platform.common.Result;
import com.manju.platform.dto.SceneGenerateRequest;
import com.manju.platform.dto.SceneGenerateResponse;
import com.manju.platform.service.AIService;
import com.manju.platform.service.GuestTrialService;
import com.manju.platform.service.HistoryService;
import com.manju.platform.service.SceneService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/api/scene")
public class SceneController {

    @Autowired
    private SceneService sceneService;
    @Autowired
    private AIService aiService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private GuestTrialService guestTrialService;

    // 提取场景描述前50字作为 input_preview
    private String extractInputPreview(String prompt) {
        if (prompt != null) {
            return prompt.length() > 50 ? prompt.substring(0, 50) + "..." : prompt;
        }
        return "";
    }

    @PostMapping("/generate")
    public Result generateScene(@RequestBody SceneGenerateRequest request, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");

        // 构建带风格声明的prompt（登录用户和试用用户都需要）
        String prompt = buildPromptWithStyle(request.getScenePrompt(), request.getStyleDeclaration());

        // 未登录试用
        if (userId == null) {
            String imageUrl = guestTrialService.execute(session,
                    Constants.TOOL_SCENE_GENERATE, Constants.DISPLAY_SCENE_GENERATE,
                    () -> aiService.generateImage(
                            prompt,
                            Collections.emptyList()
                    ));
            SceneGenerateResponse response = new SceneGenerateResponse();
            response.setImageUrl(imageUrl);
            historyService.save(null, session, Constants.TOOL_SCENE_GENERATE,
                    extractInputPreview(request.getScenePrompt()),
                    "image_url", null, imageUrl, "success", null);
            return Result.success("试用成功", response);
        }

        // 已登录用户
        SceneGenerateResponse response = sceneService.generateScene(userId, request);
        historyService.save(userId, session, Constants.TOOL_SCENE_GENERATE,
                extractInputPreview(request.getScenePrompt()),
                "image_url", null, response.getImageUrl(), "success", null);
        return Result.success("场景生成成功", response);
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
