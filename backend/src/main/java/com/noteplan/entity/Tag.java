package com.noteplan.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Tag {
    private Long id;
    private String name;
    private Integer rank;  //0普通 1重要
    private LocalDateTime createTime;
}
