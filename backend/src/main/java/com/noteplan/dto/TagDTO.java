package com.noteplan.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class TagDTO {
    private Long id;

    @NotBlank(message = "标签名不能为空")
    @Size(max = 20, message = "标签名最长20字符")
    private String name;
    private Integer rank;  // 0普通 1重要，默认0
}