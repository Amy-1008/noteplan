package com.noteplan.service;

import com.noteplan.entity.Schedule;
import com.noteplan.mapper.ScheduleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ScheduleService {

    @Autowired
    private ScheduleMapper scheduleMapper;

    public List<Schedule> getAllSchedules() {
        return scheduleMapper.findAll();
    }

    public void updateComplete(Long id, Integer completed) {
        scheduleMapper.updateComplete(id, completed);
    }
}