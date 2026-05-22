package com.noteplan.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class MonthDataDTO {
    /**
     * 按日期分组的事件
     * key: 日期字符串 (2026-05-01)
     * value: 该日期的事件列表
     */
    private Map<String, List<CalendarEventDTO>> events;

    /**
     * 总事件数
     */
    private Integer totalEvents;

    /**
     * 日程数
     */
    private Integer totalSchedules;

    /**
     * 备忘录数
     */
    private Integer totalNotes;

    /**
     * 有记录的日期数
     */
    private Integer markedDays;
}