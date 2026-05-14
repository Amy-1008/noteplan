package com.noteplan.controller;

import com.noteplan.dto.ScheduleAddDTO;
import com.noteplan.entity.Schedule;
import com.noteplan.service.ScheduleService;
import com.noteplan.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedule")
@CrossOrigin(origins = "http://localhost:5174")
//@CrossOrigin(origins = "*")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    // 获取所有日程列表
    @GetMapping("/list")
    public Result<List<Schedule>> list() {
        List<Schedule> list = scheduleService.getAllSchedules();
        return Result.success(list);
    }

    // 新增日程
    @PostMapping("/add")
    public Result<Void> addSchedule(@RequestBody ScheduleAddDTO dto) {
        scheduleService.addSchedule(dto);
        return Result.success();
    }

    // 更新完成状态
    @PutMapping("/complete")
    public Result<Void> updateComplete(@RequestParam Long id, @RequestParam Integer completed) {
        scheduleService.updateComplete(id, completed);
        return Result.success();
    }
}