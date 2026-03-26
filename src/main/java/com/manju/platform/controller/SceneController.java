package com.manju.platform.controller;


import com.manju.platform.common.Result;
import com.manju.platform.dto.SceneGenerateRequest;
import com.manju.platform.dto.SceneGenerateResponse;
import com.manju.platform.service.AIService;
import com.manju.platform.service.SceneService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/scene")
public class SceneController {
    @Autowired
    private SceneService sceneService;
    @Autowired
    private AIService aiService;

    @PostMapping("/generate")
    public Result generateScene(@RequestBody SceneGenerateRequest request, HttpSession session) {
        // 1. 获取用户ID
        Integer userId = (Integer) session.getAttribute("userId");

        // 2. 未登录试用处理
        if (userId == null) {
            Map<String, Boolean> trialMap = (Map<String, Boolean>) session.getAttribute("trialMap");
            if (trialMap == null) {
                trialMap = new HashMap<>();
            }
            if (trialMap.containsKey("scene_generate")) {
                return Result.fail("您已试用过场景生成，请登录后使用");
            }
            trialMap.put("scene_generate", true);

            // 调用真实 AI（不扣积分、不记录日志）
            String imageUrl = aiService.generateImageFromMultimodal(request.getScenePrompt(), Collections.emptyList());
            SceneGenerateResponse response = new SceneGenerateResponse();
            response.setImageUrl(imageUrl);
            return Result.success("试用成功", response);

        }
        // 3. 已登录用户正常调用 Service
        SceneGenerateResponse response = sceneService.generateScene(request);
        return Result.success("场景生成成功", response);
    }
}
