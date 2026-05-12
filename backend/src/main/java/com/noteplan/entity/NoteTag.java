package com.noteplan.entity;

import lombok.Data;

/** 关联笔记或日程与标签：targetType = NOTE | SCHEDULE */
@Data
public class NoteTag {
    private Long id;
    private Long targetId;
    private String targetType;
    private Long tagId;
}
