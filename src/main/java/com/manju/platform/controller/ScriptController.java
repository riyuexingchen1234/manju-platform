package com.manju.platform.controller;

import com.manju.platform.common.Constants;
import com.manju.platform.common.Result;
import com.manju.platform.dto.ScriptGenerateRequest;
import com.manju.platform.dto.ScriptGenerateResponse;
import com.manju.platform.service.AIService;
import com.manju.platform.service.GuestTrialService;
import com.manju.platform.service.HistoryService;
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
    @Autowired
    private HistoryService historyService;
    @Autowired
    private GuestTrialService guestTrialService;

    // 提取用户输入的第一条消息内容作为 input_preview
    private String extractInputPreview(List<Map<String, String>> messages) {
        if (messages != null && !messages.isEmpty()) {
            String content = messages.get(0).get("content");
            if (content != null) {
                return content.length() > 50 ? content.substring(0, 50) + "..." : content;
            }
        }
        return "";
    }

    @PostMapping("/generate")
    public Result generateScript(@RequestBody ScriptGenerateRequest request, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");

        // 构建消息列表
        List<Map<String, String>> messages;
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            messages = request.getMessages();
        } else if (request.getPrompt() != null) {
            messages = new ArrayList<>();
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", request.getPrompt());
            messages.add(userMsg);
        } else {
            return Result.fail("请求参数错误");
        }

        // 未登录用户试用
        if (userId == null) {
            String aiResult = guestTrialService.execute(session,
                    Constants.TOOL_SCRIPT_GENERATE, Constants.DISPLAY_SCRIPT_GENERATE,
                    () -> aiService.generateScript(messages));
            historyService.save(null, session, Constants.TOOL_SCRIPT_GENERATE,
                    extractInputPreview(messages), "text",
                    aiResult, null, "success", null);
            return Result.success("试用成功", aiResult);
        }

        // 已登录用户：正常调用 Service（积分、日志由 PaymentService 处理）
        ScriptGenerateResponse response = scriptService.generateScript(userId, messages);
        historyService.save(userId, session, Constants.TOOL_SCRIPT_GENERATE,
                extractInputPreview(messages), "text",
                response.getScript(), null, "success", null);
        return Result.success("剧本生成成功", response);
    }
}
