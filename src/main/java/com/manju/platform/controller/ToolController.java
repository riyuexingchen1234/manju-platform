package com.manju.platform.controller;

import com.manju.platform.common.Result;
import com.manju.platform.service.ToolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tool")
public class ToolController {

    @Autowired
    private ToolService toolService;

    @PostMapping("/execute")
    public Result executeTool(@RequestParam int userId,
                              @RequestParam String toolName,
                              @RequestParam String prompt) {
        return toolService.useTool(userId, toolName, prompt);
    }
}
