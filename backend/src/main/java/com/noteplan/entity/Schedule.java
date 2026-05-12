package com.noteplan.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Schedule {
    private Long id;
    private String title;
    /** 提醒时间，精确到分钟 */
    private LocalDateTime startTime;
    private String remark;
    private Integer completed;
    private Integer status;
    private Long linkedNoteId;
    private Integer rank;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
