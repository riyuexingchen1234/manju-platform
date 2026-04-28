package com.manju.platform.service;

import com.manju.platform.common.Constants;
import com.manju.platform.dto.ScriptGenerateResponse;
import com.manju.platform.entity.*;
import com.manju.platform.dao.*;
import com.manju.platform.functional.AICallable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Map;

@Service
public class ScriptService {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AIService aiService;        // 注入 AI 服务，用于真实调用

    /**
     * 剧本生成
     */
    public ScriptGenerateResponse generateScript(int userId, List<Map<String, String>> messages) {
        Object result = paymentService.processPayment(
                userId,
                Constants.TOOL_SCRIPT_GENERATE,
                Constants.POINTS_SCRIPT_GENERATE,
                () -> {
                    String aiResult = aiService.generateScript(messages);
                    ScriptGenerateResponse response = new ScriptGenerateResponse();
                    response.setContent(aiResult);
                    return response;
                }
        );
        return (ScriptGenerateResponse) result;
    }
}

