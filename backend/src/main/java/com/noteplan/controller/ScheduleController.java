package com.noteplan.controller;

import com.noteplan.entity.Schedule;
import com.noteplan.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedule")
@CrossOrigin(origins = "*")  // 允许前端跨域访问
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    // GET /api/schedule/list
    @GetMapping("/list")
    public Map<String, Object> list() {
        List<Schedule> list = scheduleService.getAllSchedules();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", list);
        return result;
    }

    @PutMapping("/complete")
    public Map<String, Object> updateComplete(@RequestParam Long id, @RequestParam Integer completed) {
        scheduleService.updateComplete(id, completed);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        return result;
    }
}