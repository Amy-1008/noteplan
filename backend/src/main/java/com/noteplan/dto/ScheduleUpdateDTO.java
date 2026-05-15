package com.noteplan.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ScheduleUpdateDTO {
    private Long id;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String repeatRule;
    private String remark;
    private Long tagId;
    private List<Long> noteIds;
}