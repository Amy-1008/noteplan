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
        List<Schedule> schedules = scheduleMapper.findAll();
        // 填充 tagId
        for (Schedule schedule : schedules) {
            Long tagId = noteTagMapper.selectTagIdByTarget(schedule.getId(), "SCHEDULE");
            schedule.setTagId(tagId);
        }
        return schedules;
    }

    // 根据ID列表获取日程
    public List<Schedule> getSchedulesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Schedule> schedules = scheduleMapper.findByIds(ids);
        // 填充 tagId
        for (Schedule schedule : schedules) {
            Long tagId = noteTagMapper.selectTagIdByTarget(schedule.getId(), "SCHEDULE");
            schedule.setTagId(tagId);
        }
        return schedules;
    }

    @Transactional
    public void addSchedule(ScheduleAddDTO dto) {
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

        // 关联标签（单选，直接处理单个 tagId）
        if (dto.getTagId() != null) {
            NoteTag noteTag = new NoteTag();
            noteTag.setTargetId(scheduleId);
            noteTag.setTargetType("SCHEDULE");
            noteTag.setTagId(dto.getTagId());
            noteTagMapper.insert(noteTag);
        }

        // 关联笔记（多选）
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
        LocalDateTime next = current;
        switch (repeatRule) {
            case "daily": next = current.plusDays(1); break;
            case "weekly": next = current.plusWeeks(1); break;
            case "monthly": next = current.plusMonths(1); break;
            case "yearly": next = current.plusYears(1); break;
            case "workday":
                next = current.plusDays(1);
                while (next.getDayOfWeek().getValue() >= 6) {
                    next = next.plusDays(1);
                }
                break;
            case "holiday":
                next = current.plusDays(1);
                while (next.getDayOfWeek().getValue() < 6) {
                    next = next.plusDays(1);
                }
                break;
            default: return current;
        }
        return next;
    }

    // 获取下一个工作日（周一至周五）
    private LocalDateTime getNextWorkday(LocalDateTime date) {
        LocalDateTime next = date.plusDays(1);
        while (next.getDayOfWeek().getValue() >= 6) { // 周六=6，周日=7
            next = next.plusDays(1);
        }
        return next;
    }

    // 获取下一个节假日（周六或周日）
    private LocalDateTime getNextHoliday(LocalDateTime date) {
        LocalDateTime next = date.plusDays(1);
        while (next.getDayOfWeek().getValue() < 6) { // 周一到周五
            next = next.plusDays(1);
        }
        return next;
    }

    public Schedule getById(Long id) {
        Schedule schedule = scheduleMapper.findById(id);
        if (schedule != null) {
            Long tagId = noteTagMapper.selectTagIdByTarget(id, "SCHEDULE");
            schedule.setTagId(tagId);
        }
        return schedule;
    }

    @Transactional
    public void updateSchedule(ScheduleUpdateDTO dto) {
        // 1. 更新日程基本信息
        Schedule schedule = scheduleMapper.findById(dto.getId());
        if (schedule == null) {
            throw new RuntimeException("日程不存在");
        }

        schedule.setTitle(dto.getTitle());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setRepeatRule(dto.getRepeatRule());
        schedule.setRemark(dto.getRemark());
        scheduleMapper.update(schedule);

        // 2. 更新标签关联
        noteTagMapper.deleteByTarget(dto.getId(), "SCHEDULE");
        if (dto.getTagId() != null) {
            NoteTag noteTag = new NoteTag();
            noteTag.setTargetId(dto.getId());
            noteTag.setTargetType("SCHEDULE");
            noteTag.setTagId(dto.getTagId());
            noteTagMapper.insert(noteTag);
        }

        // 3. 更新笔记关联
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
            return null;
        }

        ScheduleDetailVO vo = new ScheduleDetailVO();
        BeanUtils.copyProperties(schedule, vo);

        // 查询关联的标签ID（单个）
        NoteTag noteTag = noteTagMapper.selectByTarget(id, "SCHEDULE");
        vo.setTagId(noteTag != null ? noteTag.getTagId() : null);

        // 查询关联的笔记ID（多个）
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
            // 1. 删除日程关联的标签
            noteTagMapper.deleteByTarget(id, "SCHEDULE");
            // 2. 删除日程关联的笔记
            scheduleNoteMapper.deleteByScheduleId(id);
            // 3. 删除日程（软删除或硬删除）
            scheduleMapper.deleteById(id);
        }
    }
}