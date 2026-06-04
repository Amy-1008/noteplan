package com.noteplan.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class MonthDataDTO {
    private Map<String, List<CalendarEventDTO>> events;
    private Integer totalEvents;
    private Integer totalSchedules;
    private Integer totalNotes;
    private Integer markedDays;
}