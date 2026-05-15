package com.noteplan.controller;

import com.noteplan.entity.Note;
import com.noteplan.entity.NoteVersion;
import com.noteplan.service.NoteService;
import com.noteplan.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/note")
public class NoteController {

    @Autowired
    private NoteService noteService;

    @GetMapping("/list")
    public Result<List<Note>> list() {
        return Result.success(noteService.getAllNotes());
    }

    @GetMapping("/by-date")
    public Result<List<Note>> getByDate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(noteService.getNotesByDate(date));
    }

    @GetMapping("/{id}")
    public Result<Note> getById(@PathVariable Long id) {
        return Result.success(noteService.getNoteById(id));
    }

    @PostMapping("/add")
    public Result<Note> add(@Valid @RequestBody Note note) {
        return Result.success(noteService.createNote(note));
    }

    @PutMapping("/update")
    public Result<Note> update(@Valid @RequestBody Note note) {
        if (note.getId() == null) {
            return Result.error(400, "笔记ID不能为空");
        }
        return Result.success(noteService.updateNote(note));
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        noteService.deleteNote(id);
        return Result.success();
    }

    @GetMapping("/versions/{noteId}")
    public Result<List<NoteVersion>> getVersions(@PathVariable Long noteId) {
        return Result.success(noteService.getVersionsByNoteId(noteId));
    }

    @PostMapping("/recover")
    public Result<Note> recoverVersion(@RequestParam Long noteId, @RequestParam Integer versionNo) {
        return Result.success(noteService.recoverVersion(noteId, versionNo));
    }
}