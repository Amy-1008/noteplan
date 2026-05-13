package com.noteplan.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class TagDTO {
    private Long id;

    @NotBlank(message = "标签名不能为空")
    @Size(max = 100, message = "标签名最长100字符")
    private String name;
}