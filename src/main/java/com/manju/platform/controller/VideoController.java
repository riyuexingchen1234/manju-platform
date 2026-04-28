package com.manju.platform.controller;

import com.manju.platform.common.Result;
import com.manju.platform.dto.VideoGenerateRequest;
import com.manju.platform.dto.VideoGenerateResponse;
import com.manju.platform.service.AIService;
import com.manju.platform.service.VideoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/video")
public class VideoController {

    @Autowired
    private VideoService videoService;
    @Autowired
    private AIService aiService;

    // 创建视频生成任务（异步）
    @PostMapping("/generate")
    public Result generateVideo(@RequestBody VideoGenerateRequest request, HttpSession session) {
        Integer userId = (Integer)session.getAttribute("userId");
        // 未登录用户试用处理
        if (userId == null) {
            Map<String, Boolean> trialMap = (Map<String, Boolean>) session.getAttribute("trialMap");
            if (trialMap == null) {
                trialMap = new HashMap<>();
            }
            if (trialMap.containsKey("video_generate")) {
                return Result.fail("您已试用过视频生成，请登录后使用");
            }
            trialMap.put("video_generate", true);
            session.setAttribute("trialMap", trialMap);

            // 直接调用 AI 创建任务（不扣积分）
            Map<String, String> taskInfo = aiService.createVideoGenerationTask(
                    request.getKeyframeImageUrl(),
                    request.getDescription()
            );
            // 直接返回任务信息（与已登录用户的响应格式一致）
            VideoGenerateResponse response = new VideoGenerateResponse();
            response.setTaskId(taskInfo.get("taskId"));
            response.setStatus(taskInfo.get("status"));
            return Result.success("试用成功", response);
        }

        // 已登录用户，正常调用 Service（会扣积分、记日志）
        VideoGenerateResponse response = videoService.generateVideo(userId,request);
        return Result.success("视频生成成功", response);
    }

    // 查询任务结果（与登录状态无关，仅需 taskId）
    @GetMapping("/task/{taskId}")
    public Result queryVideoTask(@PathVariable String taskId){
        Map<String,Object> result = aiService.queryVideoTaskResult(taskId);
        return Result.success("查询成功",result);
    }
}