package com.manju.platform.controller;

import com.manju.platform.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/assist")
public class AssistController {
    /**
     * 小说IP搜索（模拟数据）
     */
    @GetMapping("/novel/search")
    public Result searchNovel(@RequestParam String keyword) {
        // 模拟数据（根据关键词简单过滤）
        List<Map<String, String>> results = new ArrayList<>();
        if (keyword.contains("修仙") || keyword.contains("仙侠")) {
            results.add(Map.of("title", "凡人修仙传", "author", "忘语", "intro", "凡人逆袭修仙"));
            results.add(Map.of("title", "仙逆", "author", "耳根", "intro", "逆天修仙"));
        } else if (keyword.contains("穿越") || keyword.contains("重生")) {
            results.add(Map.of("title", "庆余年", "author", "猫腻", "intro", "穿越古代权谋"));
            results.add(Map.of("title", "赘婿", "author", "愤怒的香蕉", "intro", "赘婿逆袭"));
        }
        // 默认返回几条示例
        results.add(Map.of("title", "吞噬星空", "author", "我吃西红柿", "intro", "科幻修仙"));
        results.add(Map.of("title", "斗破苍穹", "author", "天蚕土豆", "intro", "少年逆袭"));
        return Result.success("搜索成功", results);
    }

    /**
     * 漫剧榜单（模拟数据）
     */
    @GetMapping("/rank")
    public Result getRank() {
        List<Map<String, Object>> rank = List.of(
                Map.of("title", "重生之AI崛起", "hotValue", 98.5, "cover", "https://via.placeholder.com/200x120?text=Cover1"),
                Map.of("title", "修仙外卖员", "hotValue", 95.2, "cover", "https://via.placeholder.com/200x120?text=Cover2"),
                Map.of("title", "元宇宙打工人", "hotValue", 92.1, "cover", "https://via.placeholder.com/200x120?text=Cover3"),
                Map.of("title", "我的AI女友", "hotValue", 89.7, "cover", "https://via.placeholder.com/200x120?text=Cover4"),
                Map.of("title", "末世系统", "hotValue", 88.3, "cover", "https://via.placeholder.com/200x120?text=Cover5")
        );
        return Result.success("获取成功", rank);
    }

    /**
     * 对标作品拆解（模拟数据）
     */
    @GetMapping("/analyze")
    public Result analyzeWork(@RequestParam String workName) {
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("workName", workName);
        analysis.put("structure", "三幕式结构");
        analysis.put("characterCount", 5);
        analysis.put("sceneCount", 12);
        analysis.put("highlight", "反转精彩，节奏紧凑");
        analysis.put("userFeedback", "用户好评率92%");
        return Result.success("分析完成", analysis);
    }
}
