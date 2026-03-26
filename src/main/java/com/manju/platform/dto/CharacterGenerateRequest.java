package com.manju.platform.dto;
import lombok.Data;
//角色生成请求
@Data
public class CharacterGenerateRequest {
    private Integer userId;
    private String characterName;
    private String characterPrompt;     // 从拆解接口获得的完整提示词
}
