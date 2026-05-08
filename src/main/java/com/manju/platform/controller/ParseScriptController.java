package com.manju.platform.controller;

import com.manju.platform.common.Constants;
import com.manju.platform.common.Result;
import com.manju.platform.dto.ScriptParseRequest;
import com.manju.platform.dto.ScriptParseResponse;
import com.manju.platform.exception.BusinessException;
import com.manju.platform.service.AIService;
import com.manju.platform.service.GuestTrialService;
import com.manju.platform.service.HistoryService;
import com.manju.platform.service.ParseScriptService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/script")
public class ParseScriptController {
    private static final Logger logger = LoggerFactory.getLogger(ParseScriptController.class);

    @Autowired
    private ParseScriptService parseScriptService;
    @Autowired
    private AIService aiService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private GuestTrialService guestTrialService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 提取剧本前50字作为 input_preview
    private String extractInputPreview(String userScript) {
        if (userScript != null) {
            return userScript.length() > 50 ? userScript.substring(0, 50) + "..." : userScript;
        }
        return "";
    }

    // 生成拆解摘要
    private String buildParseSummary(ScriptParseResponse response) {
        int charCount = response.getCharacters() != null ? response.getCharacters().size() : 0;
        int boardCount = response.getStoryboards() != null ? response.getStoryboards().size() : 0;
        return String.format("拆解出 %d 个角色、%d 个分镜", charCount, boardCount);
    }

    @PostMapping("/parse")
    public Result parseScript(@RequestBody ScriptParseRequest request,
                              HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");

        // 未登录用户试用
        if (userId == null) {
            ScriptParseResponse response = guestTrialService.execute(
                    session, Constants.TOOL_PARSE_SCRIPT, Constants.DISPLAY_PARSE_SCRIPT,
                    () -> {
                        String rawJson = aiService.parseScript(request.getUserScript());
                        try {
                            int start = rawJson.indexOf('{');
                            int end = rawJson.lastIndexOf('}');
                            String cleanedJson = rawJson.substring(start, end + 1);
                            return objectMapper.readValue(cleanedJson, ScriptParseResponse.class);
                        } catch (Exception e) {
                            throw new BusinessException("拆解结果 JSON 解析失败");
                        }
                    }
            );
            historyService.save(null, session, Constants.TOOL_PARSE_SCRIPT,
                    extractInputPreview(request.getUserScript()),
                    "text", buildParseSummary(response), null, "success", null);
            return Result.success("试用成功", response);
        }

        // 已登录用户
        ScriptParseResponse response = parseScriptService.parseScript(userId, request.getUserScript());
        historyService.save(userId, session, Constants.TOOL_PARSE_SCRIPT,
                extractInputPreview(request.getUserScript()),
                "text", buildParseSummary(response), null, "success", null);
        return Result.success("拆解成功", response);
    }
}
