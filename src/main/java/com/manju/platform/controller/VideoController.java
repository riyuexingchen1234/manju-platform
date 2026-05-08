package com.manju.platform.controller;

import com.manju.platform.common.Constants;
import com.manju.platform.common.Result;
import com.manju.platform.dto.VideoGenerateRequest;
import com.manju.platform.dto.VideoGenerateResponse;
import com.manju.platform.entity.UserHistory;
import com.manju.platform.service.AIService;
import com.manju.platform.service.GuestTrialService;
import com.manju.platform.service.HistoryService;
import com.manju.platform.service.PaymentService;
import com.manju.platform.service.VideoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/video")
public class VideoController {

    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    private static final String VIDEO_TASK_PREFIX = "video:task:";
    private static final long VIDEO_TASK_TTL = 2 * 60 * 60; // 2小时

    @Autowired
    private VideoService videoService;
    @Autowired
    private AIService aiService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private GuestTrialService guestTrialService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

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
            // 先调用AI生成任务
            Map<String, String> taskInfo = aiService.createVideoGenerationTask(
                    request.getKeyframeImageUrl(),
                    request.getDescription()
            );
            VideoGenerateResponse response = new VideoGenerateResponse();
            response.setTaskId(taskInfo.get("taskId"));
            response.setStatus(taskInfo.get("status"));

            // 存Redis关联：taskId -> {sessionId, toolName, status, createdAt}
            String taskId = response.getTaskId();
            Map<String, Object> taskInfoRedis = new HashMap<>();
            taskInfoRedis.put("sessionId", session.getId());
            taskInfoRedis.put("toolName", Constants.TOOL_VIDEO_GENERATE);
            taskInfoRedis.put("status", "pending");
            taskInfoRedis.put("createdAt", System.currentTimeMillis());
            redisTemplate.opsForHash().putAll(VIDEO_TASK_PREFIX + taskId, taskInfoRedis);
            redisTemplate.expire(VIDEO_TASK_PREFIX + taskId, VIDEO_TASK_TTL, TimeUnit.SECONDS);

            // 保存到历史记录（pending状态）
            historyService.save(null, session, Constants.TOOL_VIDEO_GENERATE,
                    extractInputPreview(request.getDescription()),
                    "video_url", "视频生成中...", null, "pending", taskId);
            return Result.success("试用成功", response);
        }

        // 已登录用户
        VideoGenerateResponse response = videoService.generateVideo(request);
        historyService.save(userId, session, Constants.TOOL_VIDEO_GENERATE,
                extractInputPreview(request.getDescription()),
                "video_url", "视频生成中...", null, "pending", response.getTaskId());
        return Result.success("视频生成成功", response);
    }

    // 查询任务结果（与登录状态无关，仅需 taskId），完成后更新历史记录并进行延迟扣费
    @GetMapping("/task/{taskId}")
    public Result queryVideoTask(@PathVariable String taskId, HttpSession session) {
        Map<String, Object> result = aiService.queryVideoTaskResult(taskId);
        String status = (String) result.get("status");

        // 仅在任务成功或失败时进行后续处理
        if ("SUCCEEDED".equals(status)) {
            // ========== 延迟扣费逻辑（幂等：仅pending状态才扣费，防止重复轮询导致重复扣费） ==========
            Integer userId = (Integer) session.getAttribute("userId");
            if (userId != null) {
                // 已登录用户：从user_history按taskId查记录，仅pending状态才扣费
                UserHistory history = historyService.findByTaskId(taskId);
                if (history != null && "pending".equals(history.getStatus())) {
                    try {
                        paymentService.deferredProcessPayment(
                                history.getUserId(),
                                Constants.TOOL_VIDEO_GENERATE,
                                Constants.POINTS_VIDEO_GENERATE
                        );
                    } catch (Exception e) {
                        logger.error("延迟扣费失败: taskId={}, userId={}", taskId, history.getUserId(), e);
                    }
                }
            } else {
                // 未登录用户：从Redis取关联信息，仅pending状态才记录试用
                Map<Object, Object> taskInfo = redisTemplate.opsForHash().entries(VIDEO_TASK_PREFIX + taskId);
                if (!taskInfo.isEmpty() && "pending".equals(taskInfo.get("status"))) {
                    String sessionId = (String) taskInfo.get("sessionId");
                    String toolName = (String) taskInfo.get("toolName");
                    try {
                        guestTrialService.deferredTrialCheck(sessionId, toolName, session);
                        // 试用记录成功后，标记Redis状态为completed，防止重复扣费
                        redisTemplate.opsForHash().put(VIDEO_TASK_PREFIX + taskId, "status", "completed");
                    } catch (Exception e) {
                        logger.error("延迟试用判断失败: taskId={}", taskId, e);
                    }
                }
            }

            // 更新历史记录（updateByTaskId内部也有pending判断，天然幂等）
            try {
                historyService.updateByTaskId(session, taskId, "success",
                        (String) result.get("videoUrl"), "视频生成成功");
            } catch (Exception e) {
                logger.error("更新视频历史记录失败: taskId={}", taskId, e);
            }

        } else if ("FAILED".equals(status)) {
            // 视频失败：不扣费也不记录试用（试用在成功时才记录）
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
