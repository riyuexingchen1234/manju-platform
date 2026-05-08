package com.manju.platform.common;

/**
 * 常量类：统一管理工具名称和积分定价
 */
public class Constants {
    // 工具名称（对应 usage_log 表中的 tool_name 字段）
    public static final String TOOL_SCRIPT_GENERATE = "script_generate";
    public static final String TOOL_PARSE_SCRIPT = "parse_script";
    public static final String TOOL_CHARACTER_GENERATE = "character_generate";
    public static final String TOOL_SCENE_GENERATE = "scene_generate";
    public static final String TOOL_KEYFRAME_GENERATE = "keyframe_generate";
    public static final String TOOL_VIDEO_GENERATE = "video_generate";

    // 工具中文显示名称（用于用户提示信息）
    public static final String DISPLAY_SCRIPT_GENERATE = "剧本生成";
    public static final String DISPLAY_PARSE_SCRIPT = "拆解剧本";
    public static final String DISPLAY_CHARACTER_GENERATE = "角色生成";
    public static final String DISPLAY_SCENE_GENERATE = "场景生成";
    public static final String DISPLAY_KEYFRAME_GENERATE = "关键帧生成";
    public static final String DISPLAY_VIDEO_GENERATE = "视频生成";

    // 积分定价
    public static final int POINTS_SCRIPT_GENERATE = 5;
    public static final int POINTS_PARSE_SCRIPT = 5;
    public static final int POINTS_CHARACTER_GENERATE = 10;
    public static final int POINTS_SCENE_GENERATE = 10;
    public static final int POINTS_KEYFRAME_GENERATE = 10;
    public static final int POINTS_VIDEO_GENERATE = 20;
}
