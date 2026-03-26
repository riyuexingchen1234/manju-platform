package com.manju.platform.dto;
import lombok.Data;
import java.util.List;
//  拆解响应
@Data
public class ParseScriptResponse {
    private List<CharacterInfo> characters;
    private List<StoryboardInfo> storyboards;

    @Data
    public static class CharacterInfo {
        private String name;
        private String description;
        private String characterPrompt; // 用于生成角色四视图的完整提示词
    }

    @Data
    public static class StoryboardInfo {
        private String description;          // 简短描述（用于界面显示）
        private String scenePrompt;          // 场景文生图提示词
        private String detailedDescription;  // 分镜具体描述（含运镜、动作等）
        private String videoPrompt;
        private List<String> characters;     // 该分镜涉及的角色名列表
    }
}
