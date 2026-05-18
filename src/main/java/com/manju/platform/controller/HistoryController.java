package com.manju.platform.controller;

import com.manju.platform.common.Result;
import com.manju.platform.dao.HistoryDao;
import com.manju.platform.entity.UserHistory;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 历史记录控制器
 * 接口前缀：/api/history
 * 要求：需登录访问，未登录用户无历史记录入口
 */
@RestController
@RequestMapping("/api/history")
public class HistoryController {

    @Autowired
    private HistoryDao historyDao;

    /**
     * 获取最近N条历史记录（用于悬停卡片）
     */
    @GetMapping("/recent")
    public Result getRecentHistory(@RequestParam(defaultValue = "5") int limit,
                                   HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return Result.fail("请先登录");
        }
        List<UserHistory> list = historyDao.findRecentByUserId(userId, limit);
        return Result.success("查询成功", list);
    }

    /**
     * 获取全部历史记录（分页，用于弹窗）
     */
    @GetMapping("/list")
    public Result getHistoryList(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "20") int size,
                                 HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return Result.fail("请先登录");
        }
        int offset = (page - 1) * size;
        List<UserHistory> list = historyDao.findByUserId(userId, offset, size);
        int total = historyDao.countByUserId(userId);
        return Result.success("查询成功", new HistoryListResult(list, total, page, size));
    }

    // 内部类：列表查询结果
    public static class HistoryListResult {
        public List<UserHistory> list;
        public int total;
        public int page;
        public int size;

        public HistoryListResult(List<UserHistory> list, int total, int page, int size) {
            this.list = list;
            this.total = total;
            this.page = page;
            this.size = size;
        }
    }
}
