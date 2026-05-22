package com.noteplan.service.impl;

import com.noteplan.dto.CalendarEventDTO;
import com.noteplan.dto.DayDetailDTO;
import com.noteplan.dto.MonthDataDTO;
import com.noteplan.mapper.CalendarMapper;
import com.noteplan.service.CalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CalendarServiceImpl implements CalendarService {

    @Autowired
    private CalendarMapper calendarMapper;

    @Override
    public MonthDataDTO getMonthData(int year, int month) {
        MonthDataDTO result = new MonthDataDTO();
        Map<String, List<CalendarEventDTO>> eventsMap = new HashMap<>();

        // 获取统计数据
        Map<String, Object> stats = calendarMapper.getMonthStatistics(year, month);
        if (stats != null) {
            result.setTotalEvents(((Number) stats.getOrDefault("total_events", 0)).intValue());
            result.setTotalSchedules(((Number) stats.getOrDefault("total_schedules", 0)).intValue());
            result.setTotalNotes(((Number) stats.getOrDefault("total_notes", 0)).intValue());
            result.setMarkedDays(((Number) stats.getOrDefault("marked_days", 0)).intValue());
        } else {
            result.setTotalEvents(0);
            result.setTotalSchedules(0);
            result.setTotalNotes(0);
            result.setMarkedDays(0);
        }

        // 获取事件列表
        List<Map<String, Object>> events = calendarMapper.getMonthEvents(year, month);
        for (Map<String, Object> item : events) {
            String eventDate = (String) item.get("event_date");
            CalendarEventDTO event = new CalendarEventDTO();
            event.setId(String.valueOf(item.get("id")));
            event.setType((String) item.get("type"));
            event.setTitle((String) item.get("title"));
            event.setTime((String) item.get("event_time"));
            event.setStatus((String) item.get("status"));
            event.setContent((String) item.get("content"));

            eventsMap.computeIfAbsent(eventDate, k -> new ArrayList<>()).add(event);
        }

        // 按时间排序
        for (Map.Entry<String, List<CalendarEventDTO>> entry : eventsMap.entrySet()) {
            entry.getValue().sort(Comparator.comparing(e ->
                    e.getTime() != null ? e.getTime() : "99:99"
            ));
        }

        result.setEvents(eventsMap);
        return result;
    }

    @Override
    public DayDetailDTO getDayDetail(String date) {
        DayDetailDTO result = new DayDetailDTO();

        List<DayDetailDTO.ScheduleDetailDTO> schedules = new ArrayList<>();
        List<DayDetailDTO.NoteDetailDTO> notes = new ArrayList<>();

        // 查询日程详情
        List<Map<String, Object>> scheduleList = calendarMapper.getScheduleDetailByDate(date);
        for (Map<String, Object> item : scheduleList) {
            DayDetailDTO.ScheduleDetailDTO dto = new DayDetailDTO.ScheduleDetailDTO();
            dto.setId(((Number) item.get("id")).longValue());
            dto.setTitle((String) item.get("title"));
            dto.setTime((String) item.get("time_display"));
            //dto.setLocation((String) item.get("location"));
            dto.setRemark((String) item.get("remark"));
            dto.setStatus((String) item.get("status"));
            schedules.add(dto);
        }

        // 查询笔记详情
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