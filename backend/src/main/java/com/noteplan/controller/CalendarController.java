package com.noteplan.controller;

import com.noteplan.common.ApiResponse;
import com.noteplan.dto.DayDetailDTO;
import com.noteplan.dto.MonthDataDTO;
import com.noteplan.service.CalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    @Autowired
    private CalendarService calendarService;

    /**
     * 获取月份数据（日程+备忘录标记）
     * GET /api/calendar/month-data?year=2026&month=5
     */
    @GetMapping("/month-data")
    public ApiResponse<MonthDataDTO> getMonthData(
            @RequestParam int year,
            @RequestParam int month) {
        MonthDataDTO data = calendarService.getMonthData(year, month);
        return ApiResponse.ok(data);
    }

    /**
     * 获取某天详情
     * GET /api/calendar/day-detail?date=2026-05-15
     */
    @GetMapping("/day-detail")
    public ApiResponse<DayDetailDTO> getDayDetail(
            @RequestParam String date) {
        DayDetailDTO data = calendarService.getDayDetail(date);
        return ApiResponse.ok(data);
    }
}