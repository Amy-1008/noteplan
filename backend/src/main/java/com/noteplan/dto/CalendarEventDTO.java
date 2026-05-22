package com.noteplan.dto;

import lombok.Data;

@Data
public class CalendarEventDTO {
    private String id;           // 事件ID
    private String type;         // schedule / note
    private String title;        // 标题
    private String time;         // 时间（仅日程）
    private String content;      // 内容（仅备忘录）
    private String status;       // pending/completed（仅日程）
}