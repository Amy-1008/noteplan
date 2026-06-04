package com.noteplan.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ScheduleDetailVO {
    private Long id;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String repeatRule;
    private String remark;
    private Integer completed;
    private Integer status;
    private Integer rank;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long tagId;
    private List<Long> noteIds;
}