package com.noteplan.service.impl;

import com.noteplan.entity.Note;
import com.noteplan.entity.NoteVersion;
import com.noteplan.mapper.NoteMapper;
import com.noteplan.mapper.NoteTagMapper;
import com.noteplan.mapper.ScheduleNoteMapper;
import com.noteplan.service.NoteService;
import com.noteplan.service.TagService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class NoteServiceImpl implements NoteService {

    private static final Logger log = LoggerFactory.getLogger(NoteServiceImpl.class);

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private TagService tagService;

    @Autowired
    private NoteTagMapper noteTagMapper;

    @Autowired
    private ScheduleNoteMapper scheduleNoteMapper;

    private static final int MAX_VERSIONS = 3;

    @Override
    @Transactional
    public Note createNote(Note note) {
        if (note.getTitle() == null || note.getTitle().trim().isEmpty()) {
            String content = Objects.toString(note.getContent(), "").trim();
            if (content.isEmpty()) {
                content = "无标题笔记";
            }
            String title = content.length() > 50 ? content.substring(0, 50) : content;
            note.setTitle(title);
        }
        note.setCreateTime(LocalDateTime.now());
        note.setUpdateTime(LocalDateTime.now());
        note.setStatus(0);
        note.setRank(0);
        noteMapper.insert(note);

        Long currentTagId = noteTagMapper.selectTagIdByTarget(note.getId(), "NOTE");
        saveVersion(note.getId(), note.getContent(), 1, currentTagId, note.getTitle());
        log.info("创建笔记成功: id={}, title={}", note.getId(), note.getTitle());
        return note;
    }

    @Override
    @Transactional
    public Note updateNote(Note note) {
        Note oldNote = noteMapper.selectById(note.getId());
        if (oldNote == null) {
            throw new RuntimeException("笔记不存在: id=" + note.getId());
        }

        Integer maxVersionNo = noteMapper.getMaxVersionNo(note.getId());
        int nextVersionNo;
        if (maxVersionNo == null || maxVersionNo == 0) {
            // 第一次修改，保存当前内容为版本1
            nextVersionNo = 1;
        } else {
            nextVersionNo = maxVersionNo + 1;
        }
        Long currentTagId = noteTagMapper.selectTagIdByTarget(note.getId(), "NOTE");
        saveVersion(note.getId(), oldNote.getContent(), nextVersionNo, currentTagId, oldNote.getTitle());

        note.setRank(oldNote.getRank());
        note.setUpdateTime(LocalDateTime.now());
        noteMapper.update(note);

        log.info("更新笔记成功: id={}, newVersionNo={}", note.getId(), nextVersionNo);
        return note;
    }

    @Override
    @Transactional
    public void deleteNote(Long id) {
        Note note = noteMapper.selectById(id);
        if (note == null) {
            throw new RuntimeException("笔记不存在: id=" + id);
        }
        scheduleNoteMapper.deleteByNoteId(id);
        noteMapper.deleteAllVersions(id);
        noteMapper.softDeleteById(id);
        noteTagMapper.deleteByTarget(id, "NOTE");
        log.info("删除笔记成功: id={}", id);
    }

    @Override
    public Note getNoteById(Long id) {
        Note note = noteMapper.selectById(id);
        if (note == null) {
            throw new RuntimeException("笔记不存在: id=" + id);
        }
        return note;
    }

    @Override
    public List<Note> getAllNotes() {
        return noteMapper.selectAll();
    }

    @Override
    public List<Note> getNotesByDate(LocalDate date) {
        return noteMapper.selectByDate(date);
    }

    @Override
    public List<NoteVersion> getVersionsByNoteId(Long noteId) {
        getNoteById(noteId);
        return noteMapper.selectVersionsByNoteId(noteId);
    }

    @Override
    @Transactional
    public Note recoverVersion(Long noteId, Integer versionNo) {
        Note currentNote = getNoteById(noteId);
        NoteVersion targetVersion = noteMapper.selectVersion(noteId, versionNo);
        if (targetVersion == null) {
            throw new RuntimeException("版本不存在: noteId=" + noteId + ", versionNo=" + versionNo);
        }

        // 保存当前内容为新版本（快照）
        int nextVersionNo = noteMapper.getMaxVersionNo(noteId) + 1;
        Long currentTagId = noteTagMapper.selectTagIdByTarget(noteId, "NOTE");
        saveVersion(noteId, currentNote.getContent(), nextVersionNo, currentTagId, currentNote.getTitle());

        // 恢复内容、标题和标签
        currentNote.setContent(targetVersion.getContent());
        if (targetVersion.getTitle() != null && !targetVersion.getTitle().trim().isEmpty()) {
            currentNote.setTitle(targetVersion.getTitle());
        }
        currentNote.setUpdateTime(LocalDateTime.now());
        noteMapper.update(currentNote);

        // 恢复标签（利用 TagService）
        if (targetVersion.getTagId() != null) {
            tagService.bindTag(noteId, "NOTE", targetVersion.getTagId());
        } else {
            tagService.clearTag(noteId, "NOTE");
        }

        log.info("恢复笔记版本: noteId={}, fromVersionNo={}, newVersionNo={}", noteId, versionNo, nextVersionNo);
        return currentNote;
    }

    @Override
    @Transactional
    public void deleteVersion(Long noteId, Integer versionNo) {
        getNoteById(noteId);
        NoteVersion targetVersion = noteMapper.selectVersion(noteId, versionNo);
        if (targetVersion == null) {
            throw new RuntimeException("版本不存在: noteId=" + noteId + ", versionNo=" + versionNo);
        }
        noteMapper.deleteVersion(noteId, versionNo);
        log.info("删除笔记版本成功: noteId={}, versionNo={}", noteId, versionNo);
    }

    @Override
    public List<Note> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllNotes();
        }
        return noteMapper.search(keyword.trim());
    }

    private void saveVersion(Long noteId, String content, int versionNo, Long tagId, String title) {
        NoteVersion version = new NoteVersion();
        version.setNoteId(noteId);
        version.setVersionNo(versionNo);
        version.setContent(content);
        version.setSaveTime(LocalDateTime.now());
        version.setTagId(tagId); 
        version.setTitle(title);
        noteMapper.insertVersion(version);

        List<NoteVersion> versions = noteMapper.selectVersionsByNoteId(noteId);
        if (versions.size() > MAX_VERSIONS) {
            NoteVersion oldest = versions.get(versions.size() - 1);
            noteMapper.deleteVersion(noteId, oldest.getVersionNo());
            log.debug("删除最旧版本: noteId={}, versionNo={}", noteId, oldest.getVersionNo());
        }
    }
}