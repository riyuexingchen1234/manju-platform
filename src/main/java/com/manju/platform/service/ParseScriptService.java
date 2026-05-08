package com.manju.platform.service;

import com.manju.platform.dto.ScriptParseResponse;
import com.manju.platform.exception.BusinessException;
import com.manju.platform.common.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ParseScriptService {
    private static final Logger logger = LoggerFactory.getLogger(ParseScriptService.class);
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private AIService aiService;
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 拆解剧本
     */
    public ScriptParseResponse parseScript(int userId, String userScript) {
        return paymentService.processPayment(
                userId,
                Constants.TOOL_PARSE_SCRIPT,
                Constants.POINTS_PARSE_SCRIPT,
                () -> {
                    // 调用 AI 拆解剧本，获取原始 JSON 字符串
                    String rawJson = aiService.parseScript(userScript);
                    logger.debug("原始 rawJson: {}", rawJson);
                    // 检查 JSON 是否以 } 结尾
                    String trimmed = rawJson.trim();
                    if (!trimmed.endsWith("}")) {
                        throw new BusinessException("AI 生成的拆解结果不完整，请尝试缩短剧本或重新拆解");
                    }
                    // 提取第一个 '{' 到最后一个 '}' 之间的内容
                    int start = rawJson.indexOf('{');
                    int end = rawJson.lastIndexOf('}');
                    String cleanedJson = rawJson.substring(start, end + 1);
                    try {
                        return objectMapper.readValue(cleanedJson, ScriptParseResponse.class);
                    } catch (Exception e) {
                        throw new RuntimeException("拆解结果 JSON 解析失败", e);
                    }
                }
        );
    }
}
