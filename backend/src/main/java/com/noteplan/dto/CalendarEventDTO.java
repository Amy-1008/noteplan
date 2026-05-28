package com.noteplan.dto;

import lombok.Data;

@Data
public class CalendarEventDTO {
    private String id;
    private String type;
    private String title;
    private String time;
    private String status;
    private String content;
    private Integer durationDays;
    private Boolean isStartDay;
    private Boolean isEndDay;
    private Boolean isMiddleDay;
}