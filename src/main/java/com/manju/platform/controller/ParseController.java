package com.manju.platform.controller;

import com.manju.platform.common.Result;
import com.manju.platform.dto.ParseScriptRequest;
import com.manju.platform.dto.ParseScriptResponse;
import com.manju.platform.service.AIService;
import com.manju.platform.service.ParseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/script")
public class ParseController {

    @Autowired
    private ParseService parseService;

    @Autowired
    private AIService aiService;   // 未登录时直接调用

    private final ObjectMapper objectMapper = new ObjectMapper();  // 用于解析 JSON 字符串

    @PostMapping("/parse")
    public Result parseScript(@RequestBody ParseScriptRequest request,
                              HttpSession session) {
        // 1. 从 Session 中获取用户ID
        Integer userId = (Integer) session.getAttribute("userId");

        // 2. 未登录用户试用处理
        if (userId == null) {
            // 获取或创建试用记录 Map
            Map<String, Boolean> trialMap = (Map<String, Boolean>) session.getAttribute("trialMap");
            if (trialMap == null) {
                trialMap = new HashMap<>();
            }
            // 检查是否已试用过拆解剧本
            if (trialMap.containsKey("parse_script")) {
                return Result.fail("您已试用过拆解剧本，请登录后使用");
            }
            // 记录试用
            trialMap.put("parse_script", true);
            session.setAttribute("trialMap", trialMap);

            // 调用真实 AI 进行拆解（返回原始 JSON 字符串）
            String rawJson = aiService.parseScript(request.getUserScript());

            // 将 JSON 字符串解析为 ParseScriptResponse 对象
            try {
                ParseScriptResponse response = objectMapper.readValue(rawJson, ParseScriptResponse.class);
                return Result.success("试用成功", response);
            } catch (Exception e) {
                e.printStackTrace();
                return Result.fail("拆解结果解析失败");
            }
        }

        // 3. 已登录用户，正常调用 Service（会扣积分、记日志）
        ParseScriptResponse response = parseService.parseScript(userId, request.getUserScript());
        return Result.success("拆解成功", response);
    }
}