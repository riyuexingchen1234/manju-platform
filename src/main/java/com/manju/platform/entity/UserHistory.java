package com.manju.platform.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户历史记录实体
 * 对应表：user_history
 */
@Data
public class UserHistory {
    private int id;
    private int userId;
    private String toolType;        // script_generate / script_parse / character_generate / scene_generate / keyframe_generate / video_generate
    private String inputPreview;    // 输入内容缩略
    private String resultType;      // text / image_url / video_url
    private String resultText;      // 文本结果
    private String resultUrl;       // 图片/视频URL
    private String status;          // pending / success / failed
    private String taskId;          // 异步任务ID（如视频生成任务）
    private LocalDateTime createdAt;
}
