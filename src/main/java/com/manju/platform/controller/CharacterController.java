package com.manju.platform.controller;

import com.manju.platform.common.Result;
import com.manju.platform.dto.CharacterGenerateRequest;
import com.manju.platform.dto.CharacterGenerateResponse;
import com.manju.platform.service.AIService;
import com.manju.platform.service.CharacterService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/character")
public class CharacterController {
    @Autowired
    private CharacterService characterService;
    @Autowired
    private AIService aiService;

    @PostMapping("/generate")
    public Result generateCharacter(@RequestBody CharacterGenerateRequest request, HttpSession session){
        // 1. 从 Session 中获取用户ID
        Integer userId = (Integer) session.getAttribute("userId");
        // 2. 未登录用户试用处理
        if (userId == null){
            Map<String,Boolean> trialMap = (Map<String, Boolean>) session.getAttribute("trialMap");
            if (trialMap == null) {
                trialMap = new HashMap<>();
            }
            if (trialMap.containsKey("character_generate")) {
                return Result.fail("您已试用过角色生成，请登录后使用");
            }
            trialMap.put("character_generate", true);
            session.setAttribute("trialMap", trialMap);

            // 调用真实AI生成角色图（不扣积分）
            String imageUrl = aiService.generateImageFromMultimodal(
                    request.getCharacterPrompt(),
                    Collections.emptyList()
            );
            // 直接返回图片URL
            return Result.success("试用成功", imageUrl);
        }
        // 3. 已登录用户，正常调用 Service（会处理免费次数和积分）
        CharacterGenerateResponse response = characterService.generateCharacter(userId,request);
        return Result.success("角色生成成功", response);
    }

}
