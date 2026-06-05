package com.noteplan.service;

import com.noteplan.dto.ScheduleAddDTO;
import com.noteplan.dto.ScheduleUpdateDTO;
import com.noteplan.entity.NoteTag;
import com.noteplan.entity.Schedule;
import com.noteplan.mapper.NoteTagMapper;
import com.noteplan.mapper.ScheduleMapper;
import com.noteplan.mapper.ScheduleNoteMapper;
import com.noteplan.vo.ScheduleDetailVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Autowired
    private ScheduleNoteMapper scheduleNoteMapper;

    @Autowired
    private NoteTagMapper noteTagMapper;

    //批量填充日程的标签ID
    private void fillTagIds(List<Schedule> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return;
        }

        for (Schedule schedule : schedules) {
            Long tagId = noteTagMapper.selectTagIdByTarget(schedule.getId(), "SCHEDULE");
            schedule.setTagId(tagId);
        }
    }

    public List<Schedule> getAllSchedules() {
        List<Schedule> schedules = scheduleMapper.findAll();
        fillTagIds(schedules);
        return schedules;
    }

    // 根据ID列表获取日程
    public List<Schedule> getSchedulesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Schedule> schedules = scheduleMapper.findByIds(ids);
        fillTagIds(schedules);
        return schedules;
    }

    @Transactional
    public void addSchedule(ScheduleAddDTO dto) {
        // 时间合法性校验
        validateScheduleTime(dto.getStartTime(), dto.getEndTime());

        // 插入日程
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

        // 关联标签
        if (dto.getTagId() != null) {
            NoteTag noteTag = new NoteTag();
            noteTag.setTargetId(scheduleId);
            noteTag.setTargetType("SCHEDULE");
            noteTag.setTagId(dto.getTagId());
            noteTagMapper.insert(noteTag);
        }

        // 关联笔记
        if (dto.getNoteIds() != null && !dto.getNoteIds().isEmpty()) {
            for (Long noteId : dto.getNoteIds()) {
                scheduleNoteMapper.insert(scheduleId, noteId);
            }
        }
    }

    //校验日程时间的合法性
    private void validateScheduleTime(LocalDateTime startTime, LocalDateTime endTime) {
        // 结束时间不能为空
        if (endTime == null) {
            throw new IllegalArgumentException("结束时间不能为空");
        }

        // 开始时间不能晚于结束时间
        if (startTime != null && startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("开始时间不能晚于结束时间");
        }
    }

    @Transactional
    public void updateComplete(Long id, Integer completed) {
        Schedule schedule = scheduleMapper.findById(id);
        if (schedule == null) {
            throw new IllegalArgumentException("日程不存在");
        }

        // 更新 completed 状态
        scheduleMapper.updateComplete(id, completed);

        // 如果是重复日程且标记为完成，则更新时间为下一次
        if (completed == 1 && !"none".equals(schedule.getRepeatRule())) {
            LocalDateTime newEndTime = calculateNextTime(schedule.getEndTime(), schedule.getRepeatRule());
            LocalDateTime newStartTime = schedule.getStartTime() != null
                    ? calculateNextTime(schedule.getStartTime(), schedule.getRepeatRule())
                    : null;
            scheduleMapper.updateTime(id, newStartTime, newEndTime);
            // 重新将 completed 设为 0
            scheduleMapper.updateComplete(id, 0);
        }
    }

    private LocalDateTime calculateNextTime(LocalDateTime current, String repeatRule) {
        if (current == null) {
            return null;
        }

        LocalDateTime next;
        switch (repeatRule) {
            case "daily":
                next = current.plusDays(1);
                break;
            case "weekly":
                next = current.plusWeeks(1);
                break;
            case "monthly":
                next = current.plusMonths(1);
                break;
            case "yearly":
                next = current.plusYears(1);
                break;
            case "workday":
                next = getNextWorkday(current);
                break;
            case "holiday":
                next = getNextHoliday(current);
                break;
            default:
                return current;
        }
        return next;
    }

    // 获取下一个工作日
    private LocalDateTime getNextWorkday(LocalDateTime date) {
        LocalDateTime next = date.plusDays(1);
        while (next.getDayOfWeek().getValue() >= 6) { // 周六=6，周日=7
            next = next.plusDays(1);
        }
        return next;
    }

    // 获取下一个节假日
    private LocalDateTime getNextHoliday(LocalDateTime date) {
        LocalDateTime next = date.plusDays(1);
        while (next.getDayOfWeek().getValue() < 6) { // 周一到周五
            next = next.plusDays(1);
        }
        return next;
    }

    //根据ID获取日程
    private Schedule getById(Long id) {
        Schedule schedule = scheduleMapper.findById(id);
        if (schedule != null) {
            Long tagId = noteTagMapper.selectTagIdByTarget(id, "SCHEDULE");
            schedule.setTagId(tagId);
        }
        return schedule;
    }

    @Transactional
    public void updateSchedule(ScheduleUpdateDTO dto) {
        // 更新日程基本信息
        Schedule schedule = scheduleMapper.findById(dto.getId());
        if (schedule == null) {
            throw new IllegalArgumentException("日程不存在");
        }

        // 时间合法性校验
        validateScheduleTime(dto.getStartTime(), dto.getEndTime());

        // 更新日程基本信息
        schedule.setTitle(dto.getTitle());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setRepeatRule(dto.getRepeatRule());
        schedule.setRemark(dto.getRemark());
        scheduleMapper.update(schedule);

        // 更新标签关联
        noteTagMapper.deleteByTarget(dto.getId(), "SCHEDULE");
        if (dto.getTagId() != null) {
            NoteTag noteTag = new NoteTag();
            noteTag.setTargetId(dto.getId());
            noteTag.setTargetType("SCHEDULE");
            noteTag.setTagId(dto.getTagId());
            noteTagMapper.insert(noteTag);
        }

        // 更新笔记关联
        scheduleNoteMapper.deleteByScheduleId(dto.getId());
        if (dto.getNoteIds() != null && !dto.getNoteIds().isEmpty()) {
            for (Long noteId : dto.getNoteIds()) {
                scheduleNoteMapper.insert(dto.getId(), noteId);
            }
        }
    }

    public ScheduleDetailVO getDetailById(Long id) {
        Schedule schedule = scheduleMapper.findById(id);
        if (schedule == null) {
            throw new IllegalArgumentException("日程不存在");
        }

        ScheduleDetailVO vo = new ScheduleDetailVO();
        BeanUtils.copyProperties(schedule, vo);

        // 查询关联的标签ID
        NoteTag noteTag = noteTagMapper.selectByTarget(id, "SCHEDULE");
        vo.setTagId(noteTag != null ? noteTag.getTagId() : null);

        // 查询关联的笔记ID
        List<Long> noteIds = scheduleNoteMapper.findNoteIdsByScheduleId(id);
        vo.setNoteIds(noteIds != null ? noteIds : List.of());

        return vo;
    }

    @Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            // 删除日程关联的标签
            noteTagMapper.deleteByTarget(id, "SCHEDULE");
            // 删除日程关联的笔记
            scheduleNoteMapper.deleteByScheduleId(id);
            // 删除日程
            scheduleMapper.deleteById(id);
        }
    }
}