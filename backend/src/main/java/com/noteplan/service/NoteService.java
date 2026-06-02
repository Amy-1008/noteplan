package com.noteplan.service;

import com.noteplan.entity.Note;
import com.noteplan.entity.NoteVersion;

import java.time.LocalDate;
import java.util.List;

public interface NoteService {
    // 笔记 CRUD
    Note createNote(Note note);
    Note updateNote(Note note);
    void deleteNote(Long id);
    Note getNoteById(Long id);
    List<Note> getAllNotes();
    List<Note> getNotesByDate(LocalDate date);

    // 版本管理
    List<NoteVersion> getVersionsByNoteId(Long noteId);
    Note recoverVersion(Long noteId, Integer versionNo);
    void deleteVersion(Long noteId, Integer versionNo);

    List<Note> search(String keyword);
}