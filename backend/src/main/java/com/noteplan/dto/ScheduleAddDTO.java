package com.noteplan.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ScheduleAddDTO {
    @NotBlank(message = "标题不能为空")
    @Size(max = 20, message = "标题不能超过20个字符")
    private String title;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String repeatRule;

    @Size(max = 800, message = "备注不能超过800个字符")
    private String remark;

    private Long tagId;

    private List<Long> noteIds;
}