package com.manju.platform.controller;

import com.manju.platform.common.Result;
import com.manju.platform.dao.HistoryDao;
import com.manju.platform.entity.UserHistory;
import com.manju.platform.service.HistoryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;

/**
 * 历史记录控制器
 * 接口前缀：/api/history
 */
@RestController
@RequestMapping("/api/history")
public class HistoryController {

    @Autowired
    private HistoryDao historyDao;
    @Autowired
    private HistoryService historyService;

    /**
     * 获取最近N条历史记录（用于悬停卡片）
     */
    @GetMapping("/recent")
    public Result getRecentHistory(@RequestParam(defaultValue = "5") int limit,
                                   HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            List<UserHistory> guestList = historyService.getGuestHistory(session);
            if (guestList == null || guestList.isEmpty()) {
                return Result.success("暂无历史记录", new ArrayList<>());
            }
            int actualLimit = Math.min(limit, guestList.size());
            return Result.success("查询成功", guestList.subList(0, actualLimit));
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
            List<UserHistory> guestList = historyService.getGuestHistory(session);
            if (guestList == null || guestList.isEmpty()) {
                return Result.success("暂无历史记录", new HistoryListResult(new ArrayList<>(), 0, page, size));
            }

            int total = guestList.size();
            int offset = (page - 1) * size;
            if (offset >= total) {
                return Result.success("查询成功", new HistoryListResult(new ArrayList<>(), total, page, size));
            }
            int end = Math.min(offset + size, total);
            List<UserHistory> pagedList = guestList.subList(offset, end);
            return Result.success("查询成功", new HistoryListResult(pagedList, total, page, size));
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
