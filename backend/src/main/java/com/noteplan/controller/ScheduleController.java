package com.noteplan.controller;

import com.noteplan.entity.Schedule;
import com.noteplan.service.ScheduleService;
import com.noteplan.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedule")
@CrossOrigin(origins = "*")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    // GET /api/schedule/list
    @GetMapping("/list")
    public Result<List<Schedule>> list() {
        List<Schedule> list = scheduleService.getAllSchedules();
        return Result.success(list);
    }

    // PUT /api/schedule/complete
    @PutMapping("/complete")
    public Result<Void> updateComplete(@RequestParam Long id, @RequestParam Integer completed) {
        scheduleService.updateComplete(id, completed);
        return Result.success();
    }
}