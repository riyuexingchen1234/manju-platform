package com.manju.platform.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户历史记录实体
 * 对应表：user_history
 * 存储策略：登录用户 userId=真实ID；未登录用户 userId=null，通过 sessionId 追踪
 * 结果类型由 tool 字段决定：script/parse→文本，character/scene/keyframe→图片，video→视频
 */
@Data
public class UserHistory {
    private int id;
    private Integer userId;         // 登录用户ID，未登录时为 null
    private String tool;            // script_generate / parse_script / character_generate / scene_generate / keyframe_generate / video_generate
    private String inputPreview;    // 用户输入内容缩略（前50字）
    private String resultText;      // 文本结果（剧本全文、拆解摘要）
    private String resultUrl;       // 图片/视频URL
    private String status;          // pending / success / failed
    private String taskId;          // 异步任务ID（仅视频生成）
    private String sessionId;       // 追踪未登录会话，登录后用于归属合并
    private LocalDateTime createdAt;
}
