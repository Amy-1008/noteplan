package com.noteplan.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ScheduleAddDTO {
    private String title;
    private LocalDateTime startTime;   // 可为 null（时间点时）
    private LocalDateTime endTime;     // 不为 null
    private String repeatRule;         // 默认 "none"
    private String remark;
    private List<Long> tagIds;         // 关联的标签ID列表
    private List<Long> noteIds;        // 关联的笔记ID列表
}