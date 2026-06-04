package com.noteplan.service.impl;

import com.noteplan.dto.CalendarEventDTO;
import com.noteplan.dto.DayDetailDTO;
import com.noteplan.dto.MonthDataDTO;
import com.noteplan.mapper.CalendarMapper;
import com.noteplan.service.CalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class CalendarServiceImpl implements CalendarService {

    @Autowired
    private CalendarMapper calendarMapper;

    private String getDateString(Object dateObj) {
        if (dateObj == null) return null;
        String dateStr;
        if (dateObj instanceof java.sql.Date) {
            dateStr = ((java.sql.Date) dateObj).toString();
        } else if (dateObj instanceof java.util.Date) {
            dateStr = new java.sql.Date(((java.util.Date) dateObj).getTime()).toString();
        } else {
            dateStr = String.valueOf(dateObj);
        }
        if (dateStr != null && dateStr.contains("T")) {
            dateStr = dateStr.split("T")[0];
        }
        return dateStr;
    }

    private List<String> getDateRange(String startDate, String endDate) {
        List<String> dates = new ArrayList<>();
        if (startDate == null || endDate == null) return dates;
        if (startDate.contains("T")) startDate = startDate.split("T")[0];
        if (endDate.contains("T")) endDate = endDate.split("T")[0];
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        LocalDate current = start;
        while (!current.isAfter(end)) {
            dates.add(current.toString());
            current = current.plusDays(1);
        }
        return dates;
    }

    @Override
    public MonthDataDTO getMonthData(int year, int month) {
        MonthDataDTO result = new MonthDataDTO();
        Map<String, List<CalendarEventDTO>> eventsMap = new HashMap<>();
        Set<String> markedDaysSet = new HashSet<>();

        List<Map<String, Object>> schedules = calendarMapper.getSchedulesByMonth(year, month);

        Map<Long, Map<String, Object>> scheduleGroup = new HashMap<>();
        for (Map<String, Object> item : schedules) {
            Long id = ((Number) item.get("id")).longValue();
            if (!scheduleGroup.containsKey(id)) {
                scheduleGroup.put(id, new HashMap<>());
            }
            Map<String, Object> existing = scheduleGroup.get(id);
            existing.put("id", id);
            existing.put("title", item.get("title"));
            existing.put("status", item.get("status"));

            Object startTime = item.get("start_time");
            Object endTime = item.get("end_time");
            Object eventDate = item.get("event_date");
            if (startTime != null) existing.put("start_time", startTime);
            if (endTime != null) existing.put("end_time", endTime);
            if (eventDate != null) existing.put("event_date", eventDate);
            if (item.get("event_time") != null) existing.put("event_time", item.get("event_time"));
        }

        for (Map<String, Object> schedule : scheduleGroup.values()) {
            Long id = (Long) schedule.get("id");
            String title = (String) schedule.get("title");
            String status = (String) schedule.get("status");
            Object startTime = schedule.get("start_time");
            Object endTime = schedule.get("end_time");
            Object eventDateObj = schedule.get("event_date");

            if (startTime != null && endTime != null) {
                String startDate = getDateString(startTime);
                String endDate = getDateString(endTime);

                if (startDate != null && endDate != null && !startDate.equals(endDate)) {
                    List<String> dateRange = getDateRange(startDate, endDate);
                    for (int i = 0; i < dateRange.size(); i++) {
                        String currentDate = dateRange.get(i);
                        if (currentDate == null) continue;
                        if (!currentDate.startsWith(year + "-" + String.format("%02d", month))) {
                            continue;
                        }

                        CalendarEventDTO event = new CalendarEventDTO();
                        event.setId(String.valueOf(id));
                        event.setType("schedule");
                        event.setTitle(title);
                        event.setStatus(status);

                        if (i == 0) {
                            event.setTime("开始");
                            event.setIsStartDay(true);
                            event.setIsEndDay(false);
                            event.setIsMiddleDay(false);
                        } else if (i == dateRange.size() - 1) {
                            event.setTime("结束");
                            event.setIsStartDay(false);
                            event.setIsEndDay(true);
                            event.setIsMiddleDay(false);
                        } else {
                            event.setTime("│");
                            event.setIsStartDay(false);
                            event.setIsEndDay(false);
                            event.setIsMiddleDay(true);
                        }

                        eventsMap.computeIfAbsent(currentDate, k -> new ArrayList<>()).add(event);
                        markedDaysSet.add(currentDate);
                    }
                    continue;
                }
            }

            String eventDate = eventDateObj != null ? getDateString(eventDateObj) : null;
            if (eventDate == null) continue;

            CalendarEventDTO event = new CalendarEventDTO();
            event.setId(String.valueOf(id));
            event.setType("schedule");
            event.setTitle(title);
            event.setTime((String) schedule.get("event_time"));
            event.setStatus(status);
            event.setIsStartDay(true);
            event.setIsEndDay(true);
            event.setIsMiddleDay(false);
            eventsMap.computeIfAbsent(eventDate, k -> new ArrayList<>()).add(event);
            markedDaysSet.add(eventDate);
        }

        List<Map<String, Object>> notes = calendarMapper.getNotesByMonth(year, month);
        for (Map<String, Object> item : notes) {
            String eventDate = getDateString(item.get("event_date"));
            if (eventDate == null) continue;

            CalendarEventDTO event = new CalendarEventDTO();
            event.setId(String.valueOf(item.get("id")));
            event.setType("note");
            event.setTitle((String) item.get("title"));
            event.setTime((String) item.get("event_time"));
            event.setIsStartDay(true);
            event.setIsEndDay(true);
            event.setIsMiddleDay(false);
            eventsMap.computeIfAbsent(eventDate, k -> new ArrayList<>()).add(event);
            markedDaysSet.add(eventDate);
        }

        eventsMap.remove(null);

        for (Map.Entry<String, List<CalendarEventDTO>> entry : eventsMap.entrySet()) {
            if (entry.getKey() == null) continue;
            entry.getValue().sort(Comparator.comparing(e -> {
                if (e.getIsStartDay() != null && e.getIsStartDay()) return "0";
                if (e.getIsMiddleDay() != null && e.getIsMiddleDay()) return "1";
                return "2";
            }));
        }

        int totalEvents = eventsMap.values().stream().mapToInt(List::size).sum();
        int totalSchedules = (int) scheduleGroup.values().stream().filter(s -> true).count();
        int totalNotes = notes.size();

        result.setEvents(eventsMap);
        result.setTotalEvents(totalEvents);
        result.setTotalSchedules(totalSchedules);
        result.setTotalNotes(totalNotes);
        result.setMarkedDays(markedDaysSet.size());

        return result;
    }

    @Override
    public DayDetailDTO getDayDetail(String date) {
        DayDetailDTO result = new DayDetailDTO();
        List<DayDetailDTO.ScheduleDetailDTO> schedules = new ArrayList<>();
        List<DayDetailDTO.NoteDetailDTO> notes = new ArrayList<>();

        List<Map<String, Object>> scheduleList = calendarMapper.getScheduleDetailByDate(date);
        for (Map<String, Object> item : scheduleList) {
            DayDetailDTO.ScheduleDetailDTO dto = new DayDetailDTO.ScheduleDetailDTO();
            dto.setId(((Number) item.get("id")).longValue());
            dto.setTitle((String) item.get("title"));
            dto.setTime((String) item.get("time_display"));
            dto.setRemark((String) item.get("remark"));
            dto.setStatus((String) item.get("status"));

            String tagsStr = (String) item.get("tags");
            if (tagsStr != null && !tagsStr.isEmpty()) {
                dto.setTags(Arrays.asList(tagsStr.split(",")));
            } else {
                dto.setTags(new ArrayList<>());
            }
            schedules.add(dto);
        }

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