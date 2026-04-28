package com.manju.platform.controller;


import com.manju.platform.common.Result;
import com.manju.platform.dto.KeyframeGenerateRequest;
import com.manju.platform.dto.KeyframeGenerateResponse;
import com.manju.platform.service.AIService;
import com.manju.platform.service.KeyframeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/keyframe")
public class KeyframeController {
    @Autowired
    private KeyframeService keyframeService;
    @Autowired
    private AIService aiService;

    @PostMapping("/generate")
    public Result generateKeyframe(@RequestBody KeyframeGenerateRequest request, HttpSession session){
        // 1. 从 Session 中获取用户ID
        Integer userId = (Integer) session.getAttribute("userId");

        // 2. 未登录用户试用处理
        if (userId == null) {
            // 获取或创建试用记录 Map
            Map<String, Boolean> trialMap = (Map<String, Boolean>) session.getAttribute("trialMap");
            if (trialMap == null) {
                trialMap = new HashMap<>();
            }
            // 检查是否已试用过关键帧生成
            if (trialMap.containsKey("keyframe_generate")) {
                return Result.fail("您已试用过关键帧生成，请登录后使用");
            }
            // 记录试用
            trialMap.put("keyframe_generate", true);
            session.setAttribute("trialMap", trialMap);

            // 构造融合提示词
            String prompt = String.format(
                    "使用第一张图作为角色形象，第二张图作为背景场景。请将角色自然地融入场景中，执行动作：%s。" +
                            "保持角色和场景的真实性，生成一张融合后的新图片。",
                    request.getStoryboardDescription()
            );
            // 参考图列表：角色图、场景图
            List<String> imageUrls = Arrays.asList(
                    request.getCharacterImageUrl(),
                    request.getSceneImageUrl()
            );
            // 调用真实AI
            String imageUrl = aiService.generateImageFromMultimodal(prompt, imageUrls);
            KeyframeGenerateResponse response = new KeyframeGenerateResponse();
            return Result.success("试用成功", response);
        }

        // 3. 已登录用户，正常调用 Service（会扣积分、记日志）
        KeyframeGenerateResponse response = keyframeService.generateKeyframe(userId,request);
        return  Result.success("关键帧生成成功",response);
    }
}
