package com.manju.platform.controller;

import com.manju.platform.common.Constants;
import com.manju.platform.common.Result;
import com.manju.platform.dto.VideoGenerateRequest;
import com.manju.platform.dto.VideoGenerateResponse;
import com.manju.platform.service.AIService;
import com.manju.platform.service.GuestTrialService;
import com.manju.platform.service.HistoryService;
import com.manju.platform.service.VideoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/video")
public class VideoController {

    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    @Autowired
    private VideoService videoService;
    @Autowired
    private AIService aiService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private GuestTrialService guestTrialService;

    // 提取视频描述前50字作为 input_preview
    private String extractInputPreview(String description) {
        if (description != null) {
            return description.length() > 50 ? description.substring(0, 50) + "..." : description;
        }
        return "";
    }

    // 创建视频生成任务（异步）
    @PostMapping("/generate")
    public Result generateVideo(@RequestBody VideoGenerateRequest request, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");

        // 未登录用户试用
        if (userId == null) {
            VideoGenerateResponse response = guestTrialService.execute(session,
                    Constants.TOOL_VIDEO_GENERATE, Constants.DISPLAY_VIDEO_GENERATE,
                    () -> {
                        Map<String, String> taskInfo = aiService.createVideoGenerationTask(
                                request.getKeyframeImageUrl(),
                                request.getDescription()
                        );
                        VideoGenerateResponse resp = new VideoGenerateResponse();
                        resp.setTaskId(taskInfo.get("taskId"));
                        resp.setStatus(taskInfo.get("status"));
                        return resp;
                    });
            historyService.save(null, session, Constants.TOOL_VIDEO_GENERATE,
                    extractInputPreview(request.getDescription()),
                    "video_url", "视频生成中...", null, "pending", response.getTaskId());
            return Result.success("试用成功", response);
        }

        // 已登录用户
        VideoGenerateResponse response = videoService.generateVideo(userId, request);
        historyService.save(userId, session, Constants.TOOL_VIDEO_GENERATE,
                extractInputPreview(request.getDescription()),
                "video_url", "视频生成中...", null, "pending", response.getTaskId());
        return Result.success("视频生成成功", response);
    }

    // 查询任务结果，完成后更新历史记录
    @GetMapping("/task/{taskId}")
    public Result queryVideoTask(@PathVariable String taskId, HttpSession session) {
        Map<String, Object> result = aiService.queryVideoTaskResult(taskId);
        String status = (String) result.get("status");

        if ("SUCCEEDED".equals(status)) {
            try {
                historyService.updateByTaskId(session, taskId, "success",
                        (String) result.get("videoUrl"), "视频生成成功");
            } catch (Exception e) {
                logger.error("更新视频历史记录失败: taskId={}", taskId, e);
            }
        } else if ("FAILED".equals(status)) {
            try {
                historyService.updateByTaskId(session, taskId, "failed", null,
                        result.get("error") != null ? (String) result.get("error") : "视频生成失败");
            } catch (Exception e) {
                logger.error("更新视频历史记录失败: taskId={}", taskId, e);
            }
        }

        return Result.success("查询成功", result);
    }
}
