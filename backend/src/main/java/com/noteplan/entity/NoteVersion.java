package com.noteplan.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoteVersion {
    private Long id;
    private Long noteId;
    private Integer versionNo;
    private String content;
    private LocalDateTime saveTime;
}
