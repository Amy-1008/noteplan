package com.noteplan.controller;

import com.noteplan.dto.ScheduleAddDTO;
import com.noteplan.dto.ScheduleUpdateDTO;
import com.noteplan.entity.Schedule;
import com.noteplan.service.ScheduleService;
import com.noteplan.vo.Result;
import com.noteplan.vo.ScheduleDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/schedule")
@CrossOrigin(origins = "http://localhost:5174")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    // 获取日程列表（支持 ids 参数筛选）
    @GetMapping("/list")
    public Result<List<Schedule>> list(@RequestParam(required = false) String ids) {
        List<Schedule> list;
        if (ids != null && !ids.isEmpty()) {
            // 解析 ids 参数，例如 "1,2,3"
            List<Long> idList = Arrays.stream(ids.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            list = scheduleService.getSchedulesByIds(idList);
        } else {
            list = scheduleService.getAllSchedules();
        }
        return Result.success(list);
    }

    // 新增日程
    @PostMapping("/add")
    public Result<Void> addSchedule(@RequestBody ScheduleAddDTO dto) {
        scheduleService.addSchedule(dto);
        return Result.success();
    }

    // 更新日程
    @PutMapping("/update")
    public Result<Void> updateSchedule(@RequestBody ScheduleUpdateDTO dto) {
        scheduleService.updateSchedule(dto);
        return Result.success();
    }

    // 更新完成状态
    @PutMapping("/complete")
    public Result<Void> updateComplete(@RequestParam Long id, @RequestParam Integer completed) {
        scheduleService.updateComplete(id, completed);
        return Result.success();
    }

    // 获取单个日程详情
    @GetMapping("/detail")
    public Result<ScheduleDetailVO> detail(@RequestParam Long id) {
        ScheduleDetailVO vo = scheduleService.getDetailById(id);
        return Result.success(vo);
    }
}