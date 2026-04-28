package com.manju.platform.controller;

import com.manju.platform.common.Result;

import com.manju.platform.dto.ScriptGenerateResponse;
import com.manju.platform.dto.ScriptGenerateRequest;
import com.manju.platform.service.AIService;
import com.manju.platform.service.ScriptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/script")
public class ScriptController {

    @Autowired
    private ScriptService scriptService;
    @Autowired
    private AIService aiService;

    @PostMapping("/generate")
    public Result generateScript(@RequestBody ScriptGenerateRequest request,HttpSession session) {
        // 1. 从 Session 中获取用户ID（登录时已存入）
        Integer userId = (Integer) session.getAttribute("userId");
        List<Map<String, String>> messages;
        // 构建消息列表
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            messages = request.getMessages();
        } else if (request.getPrompt() != null) {
            // 兼容单轮
            messages = new ArrayList<>();
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", request.getPrompt());
            messages.add(userMsg);
        } else {
            return Result.fail("请求参数错误");
        }

        // 2. 未登录用户试用处理
        if (userId == null){
            // 获取或创建试用记录 Map
            Map<String,Boolean>trialMap = (Map<String, Boolean>) session.getAttribute("trialMap");
            if (trialMap == null){
                trialMap = new HashMap<>();
            }
            // 检查是否已试用过剧本生成
            if (trialMap.containsKey("script_generate")){
                return Result.fail("您已试用过剧本生成，请登录后使用");
            }
            // 记录试用
            trialMap.put("script_generate",true);
            session.setAttribute("trialMap",trialMap);

            // 未登录试用直接调用 AI（不扣积分）
            String aiResult = aiService.generateScript(messages);
            return Result.success("试用成功",aiResult);
        }


        // 3. 已登录用户：正常调用 Service（积分、日志由 PaymentService 处理）
        ScriptGenerateResponse response = scriptService.generateScript(userId, messages);
        return Result.success("剧本生成成功", response);

    }

}
