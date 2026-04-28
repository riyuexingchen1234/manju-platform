package com.manju.platform.service;

import com.manju.platform.common.Constants;
import com.manju.platform.dto.ParseScriptResponse;
import com.manju.platform.exception.BusinessException;
import com.manju.platform.functional.AICallable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class ParseService {
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private AIService aiService;        // 注入 AI 服务，用于真实调用
    @Autowired
    private ObjectMapper objectMapper;  // 用于解析 JSON


    /**
     * 拆解剧本
     */
    public ParseScriptResponse parseScript(int userId, String userScript) {
        Object result = paymentService.processPayment(
                userId,
                Constants.TOOL_PARSE_SCRIPT,
                Constants.POINTS_PARSE_SCRIPT,
                () -> {
                    // 调用 AI 拆解剧本，获取原始 JSON 字符串
                    String rawJson = aiService.parseScript(userScript);
                    System.out.println("原始 rawJson: " + rawJson);  // 打印原始内容
                    // 检查 JSON 是否以 } 结尾（快速判断）
                    String trimmed = rawJson.trim();
                    if (!trimmed.endsWith("}")) {
                        throw new BusinessException("AI 生成的拆解结果不完整，请尝试缩短剧本或重新拆解");
                    }
                    // 提取第一个 '{' 到最后一个 '}' 之间的内容
                    int start = rawJson.indexOf('{');
                    int end = rawJson.lastIndexOf('}');
                    String cleanedJson = rawJson.substring(start, end + 1);
                    return objectMapper.readValue(cleanedJson, ParseScriptResponse.class);
                }
        );
        return (ParseScriptResponse) result;
    }
}
