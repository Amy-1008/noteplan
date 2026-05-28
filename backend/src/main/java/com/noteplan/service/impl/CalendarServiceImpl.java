package com.noteplan.service.impl;

import com.noteplan.dto.CalendarEventDTO;
import com.noteplan.dto.DayDetailDTO;
import com.noteplan.dto.MonthDataDTO;
import com.noteplan.mapper.CalendarMapper;
import com.noteplan.service.CalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CalendarServiceImpl implements CalendarService {

    @Autowired
    private CalendarMapper calendarMapper;

    private String getDateString(Object dateObj) {
        if (dateObj == null) return null;
        if (dateObj instanceof java.sql.Date) {
            return ((java.sql.Date) dateObj).toString();
        }
        if (dateObj instanceof java.util.Date) {
            return new java.sql.Date(((java.util.Date) dateObj).getTime()).toString();
        }
        return String.valueOf(dateObj);
    }

    @Override
    public MonthDataDTO getMonthData(int year, int month) {
        MonthDataDTO result = new MonthDataDTO();
        Map<String, List<CalendarEventDTO>> eventsMap = new HashMap<>();
        Set<String> markedDaysSet = new HashSet<>();
        int totalSchedules = 0;
        int totalNotes = 0;

        // 获取统计数据
        Map<String, Object> stats = calendarMapper.getMonthStatistics(year, month);
        if (stats != null) {
            totalSchedules = ((Number) stats.getOrDefault("total_schedules", 0)).intValue();
            totalNotes = ((Number) stats.getOrDefault("total_notes", 0)).intValue();
            result.setMarkedDays(((Number) stats.getOrDefault("marked_days", 0)).intValue());
        }
        result.setTotalEvents(totalSchedules + totalNotes);
        result.setTotalSchedules(totalSchedules);
        result.setTotalNotes(totalNotes);

        // 获取日程
        List<Map<String, Object>> schedules = calendarMapper.getSchedulesByMonth(year, month);
        for (Map<String, Object> item : schedules) {
            String eventDate = getDateString(item.get("event_date"));
            CalendarEventDTO event = new CalendarEventDTO();
            event.setId(String.valueOf(item.get("id")));
            event.setType("schedule");
            event.setTitle((String) item.get("title"));
            event.setTime((String) item.get("event_time"));
            event.setStatus((String) item.get("status"));
            eventsMap.computeIfAbsent(eventDate, k -> new ArrayList<>()).add(event);
            markedDaysSet.add(eventDate);
        }

        // 获取笔记
        List<Map<String, Object>> notes = calendarMapper.getNotesByMonth(year, month);
        for (Map<String, Object> item : notes) {
            String eventDate = getDateString(item.get("event_date"));
            CalendarEventDTO event = new CalendarEventDTO();
            event.setId(String.valueOf(item.get("id")));
            event.setType("note");
            event.setTitle((String) item.get("title"));
            event.setTime((String) item.get("event_time"));
            eventsMap.computeIfAbsent(eventDate, k -> new ArrayList<>()).add(event);
            markedDaysSet.add(eventDate);
        }

        // 按时间排序
        for (Map.Entry<String, List<CalendarEventDTO>> entry : eventsMap.entrySet()) {
            entry.getValue().sort(Comparator.comparing(e -> e.getTime() != null ? e.getTime() : "99:99"));
        }

        result.setEvents(eventsMap);
        result.setMarkedDays(markedDaysSet.size());

        return result;
    }

    @Override
    public DayDetailDTO getDayDetail(String date) {
        DayDetailDTO result = new DayDetailDTO();
        List<DayDetailDTO.ScheduleDetailDTO> schedules = new ArrayList<>();
        List<DayDetailDTO.NoteDetailDTO> notes = new ArrayList<>();

        // 日程详情
        List<Map<String, Object>> scheduleList = calendarMapper.getScheduleDetailByDate(date);
        for (Map<String, Object> item : scheduleList) {
            DayDetailDTO.ScheduleDetailDTO dto = new DayDetailDTO.ScheduleDetailDTO();
            dto.setId(((Number) item.get("id")).longValue());
            dto.setTitle((String) item.get("title"));
            dto.setTime((String) item.get("time_display"));
            dto.setRemark((String) item.get("remark"));
            dto.setStatus((String) item.get("status"));
            schedules.add(dto);
        }

        // 笔记详情
        List<Map<String, Object>> noteList = calendarMapper.getNoteDetailByDate(date);
        for (Map<String, Object> item : noteList) {
            DayDetailDTO.NoteDetailDTO dto = new DayDetailDTO.NoteDetailDTO();
            dto.setId(((Number) item.get("id")).longValue());
            dto.setTitle((String) item.get("title"));
            dto.setContent((String) item.get("content"));
            dto.setCreateTime((String) item.get("time_only"));
            String tagsStr = (String) item.get("tags");
            if (tagsStr != null && !tagsStr.isEmpty()) {
                dto.setTags(Arrays.asList(tagsStr.split(",")));
            } else {
                dto.setTags(new ArrayList<>());
            }
            notes.add(dto);
        }

        result.setSchedules(schedules);
        result.setNotes(notes);
        return result;
    }
}