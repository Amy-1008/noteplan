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

    @GetMapping("/month-data")
    public ApiResponse<MonthDataDTO> getMonthData(
            @RequestParam int year,
            @RequestParam int month) {
        MonthDataDTO data = calendarService.getMonthData(year, month);
        return ApiResponse.ok(data);
    }

    @GetMapping("/day-detail")
    public ApiResponse<DayDetailDTO> getDayDetail(@RequestParam String date) {
        DayDetailDTO data = calendarService.getDayDetail(date);
        return ApiResponse.ok(data);
    }
}