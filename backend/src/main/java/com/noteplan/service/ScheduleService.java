package com.noteplan.service;

import com.noteplan.dto.ScheduleAddDTO;
import com.noteplan.entity.NoteTag;
import com.noteplan.entity.Schedule;
import com.noteplan.mapper.NoteTagMapper;
import com.noteplan.mapper.ScheduleMapper;
import com.noteplan.mapper.ScheduleNoteMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class ScheduleService {

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Autowired
    private ScheduleNoteMapper scheduleNoteMapper;

    @Autowired
    private NoteTagMapper noteTagMapper;

    public List<Schedule> getAllSchedules() {
        return scheduleMapper.findAll();
    }

    // 根据ID列表获取日程
    public List<Schedule> getSchedulesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return scheduleMapper.findByIds(ids);
    }

    @Transactional
    public void addSchedule(ScheduleAddDTO dto) {
        // 1. 插入日程
        Schedule schedule = new Schedule();
        schedule.setTitle(dto.getTitle());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setRepeatRule(dto.getRepeatRule() != null ? dto.getRepeatRule() : "none");
        schedule.setRemark(dto.getRemark());
        schedule.setCompleted(0);
        schedule.setStatus(0);
        schedule.setRank(0);

        scheduleMapper.insert(schedule);
        Long scheduleId = schedule.getId();

        // 2. 关联标签（使用 NoteTag 实体）
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            for (Long tagId : dto.getTagIds()) {
                NoteTag noteTag = new NoteTag();
                noteTag.setTargetId(scheduleId);
                noteTag.setTargetType("SCHEDULE");
                noteTag.setTagId(tagId);
                noteTagMapper.insert(noteTag);
            }
        }

        // 3. 关联笔记
        if (dto.getNoteIds() != null && !dto.getNoteIds().isEmpty()) {
            for (Long noteId : dto.getNoteIds()) {
                scheduleNoteMapper.insert(scheduleId, noteId);
            }
        }
    }

    @Transactional
    public void updateComplete(Long id, Integer completed) {
        Schedule schedule = scheduleMapper.findById(id);
        if (schedule == null) {
            return;
        }

        if ("none".equals(schedule.getRepeatRule())) {
            scheduleMapper.updateComplete(id, completed);
        } else {
            LocalDateTime newEndTime = calculateNextTime(schedule.getEndTime(), schedule.getRepeatRule());
            LocalDateTime newStartTime = schedule.getStartTime() != null
                    ? calculateNextTime(schedule.getStartTime(), schedule.getRepeatRule())
                    : null;

            scheduleMapper.updateTime(id, newStartTime, newEndTime);
        }
    }

    private LocalDateTime calculateNextTime(LocalDateTime current, String repeatRule) {
        switch (repeatRule) {
            case "daily": return current.plusDays(1);
            case "weekly": return current.plusWeeks(1);
            case "monthly": return current.plusMonths(1);
            case "yearly": return current.plusYears(1);
            case "workday": return current.plusDays(1);
            case "holiday": return current.plusDays(1);
            default: return current;
        }
    }
}