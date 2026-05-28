package com.manju.platform.controller;

import com.manju.platform.common.Constants;
import com.manju.platform.common.PromptUtils;
import com.manju.platform.common.Result;
import com.manju.platform.dto.KeyframeGenerateRequest;
import com.manju.platform.dto.KeyframeGenerateResponse;
import com.manju.platform.service.AIService;
import com.manju.platform.service.GuestTrialService;
import com.manju.platform.service.HistoryService;
import com.manju.platform.service.KeyframeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/keyframe")
public class KeyframeController {
    @Autowired
    private KeyframeService keyframeService;
    @Autowired
    private AIService aiService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private GuestTrialService guestTrialService;

    // 提取分镜描述前50字作为 input_preview
    private String extractInputPreview(String description) {
        if (description != null) {
            return description.length() > 10 ? description.substring(0, 10) + "..." : description;
        }
        return "";
    }

    @PostMapping("/generate")
    public Result generateKeyframe(@RequestBody KeyframeGenerateRequest request, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");

        // 未登录用户试用
        if (userId == null) {
            KeyframeGenerateResponse response = guestTrialService.execute(session,
                    Constants.TOOL_KEYFRAME_GENERATE, Constants.DISPLAY_KEYFRAME_GENERATE,
                    () -> {
                        // 构建 imageUrls：先放所有角色图，再放场景图
                        List<String> imageUrls = new ArrayList<>();
                        if (request.getCharacterImageUrls() != null) {
                            imageUrls.addAll(request.getCharacterImageUrls());
                        }
                        if (request.getSceneImageUrl() != null) {
                            imageUrls.add(request.getSceneImageUrl());
                        }

                        // 构建 prompt
                        int charCount = request.getCharacterImageUrls() != null ? request.getCharacterImageUrls().size() : 0;
                        String prompt = PromptUtils.buildKeyframePrompt(request.getStoryboardDescription(), charCount);

                        String imageUrl = aiService.generateImage(prompt, imageUrls);
                        KeyframeGenerateResponse resp = new KeyframeGenerateResponse();
                        resp.setImageUrl(imageUrl);
                        return resp;
                    });
            historyService.save(null, session, Constants.TOOL_KEYFRAME_GENERATE,
                    extractInputPreview(request.getStoryboardDescription()),
                    null, response.getImageUrl(), "success", null);
            return Result.success("试用成功", response);
        }

        // 已登录用户
        KeyframeGenerateResponse response = keyframeService.generateKeyframe(userId, request);
        historyService.save(userId, session, Constants.TOOL_KEYFRAME_GENERATE,
                extractInputPreview(request.getStoryboardDescription()),
                null, response.getImageUrl(), "success", null);
        return Result.success("关键帧生成成功", response);
    }
}
