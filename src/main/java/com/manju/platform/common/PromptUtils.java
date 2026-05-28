package com.manju.platform.common;

public class PromptUtils {

    /**
     * 将全局风格声明拼接到用户 prompt 末尾
     * @param userPrompt       用户输入的提示词
     * @param styleDeclaration 全局视觉风格声明
     * @return 拼接后的完整 prompt
     */
    public static String buildPromptWithStyle(String userPrompt, String styleDeclaration) {
        if (userPrompt == null) {
            userPrompt = "";
        }
        if (styleDeclaration != null && !styleDeclaration.trim().isEmpty()) {
            return userPrompt + "\n\n【全局风格声明】" + styleDeclaration;
        }
        return userPrompt;
    }

    /**
     * 构建关键帧生成的提示词（根据角色图和场景图数量自动适配）
     * @param storyboardDescription 分镜描述
     * @param charCount            角色图数量
     * @return 给 AI 的完整 prompt
     */
    public static String buildKeyframePrompt(String storyboardDescription, int charCount) {
        String prompt;
        if (charCount == 0) {
            prompt = "请根据以下场景参考图生成关键帧图片。";
        } else if (charCount == 1) {
            prompt = "请根据以下参考图和描述生成关键帧图片。图1是角色形象参考图，图2是场景背景参考图。请将角色自然地融入场景中，";
        } else {
            prompt = String.format(
                    "请根据以下参考图和描述生成关键帧图片。图1到图%d是角色形象参考图，图%d是场景背景参考图。请将角色自然地融入场景中，",
                    charCount, charCount + 1
            );
        }
        return prompt + storyboardDescription;
    }
}