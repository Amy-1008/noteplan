package com.noteplan.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Note {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 0 正常 1 已删除 */
    private Integer status;
    private Integer rank;
}
