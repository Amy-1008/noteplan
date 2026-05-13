package com.noteplan.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class NoteTagDTO {
    @NotNull(message = "目标ID不能为空")
    private Long targetId;

    @NotBlank(message = "目标类型不能为空")
    private String targetType;  // NOTE 或 SCHEDULE

    @NotNull(message = "标签ID不能为空")
    private Long tagId;
}