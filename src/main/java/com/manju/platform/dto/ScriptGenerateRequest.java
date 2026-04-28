package com.manju.platform.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ScriptGenerateRequest {
    private String prompt;
    private List<Map<String, String>> messages; // 对话历史 [{role, content}]
}
