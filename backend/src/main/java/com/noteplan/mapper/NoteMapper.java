package com.noteplan.mapper;

import com.noteplan.entity.Note;
import com.noteplan.entity.NoteVersion;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface NoteMapper {

    // ---------- 笔记 CRUD ----------
    @Select("SELECT * FROM note WHERE id = #{id} AND status = 0")
    Note selectById(Long id);

    @Select("SELECT * FROM note WHERE status = 0 ORDER BY update_time DESC")
    List<Note> selectAll();

    @Insert("INSERT INTO note (title, content, create_time, update_time, status, rank) " +
            "VALUES (#{title}, #{content}, #{createTime}, #{updateTime}, #{status}, #{rank})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Note note);

    @Update("UPDATE note SET title = #{title}, content = #{content}, update_time = #{updateTime}, rank = #{rank} WHERE id = #{id}")
    int update(Note note);

    // 软删除（仅更新 status = 1）
    @Update("UPDATE note SET status = 1 WHERE id = #{id}")
    int softDeleteById(Long id);

    // 按日期查询笔记（根据 update_time 的年月日）
    @Select("SELECT * FROM note WHERE DATE(update_time) = #{date} AND status = 0 ORDER BY update_time DESC")
    List<Note> selectByDate(@Param("date") LocalDate date);

    // ---------- 版本管理 ----------
    @Select("SELECT * FROM note_version WHERE note_id = #{noteId} ORDER BY version_no DESC")
    List<NoteVersion> selectVersionsByNoteId(Long noteId);

    @Select("SELECT * FROM note_version WHERE note_id = #{noteId} AND version_no = #{versionNo}")
    NoteVersion selectVersion(@Param("noteId") Long noteId, @Param("versionNo") Integer versionNo);

    @Insert("INSERT INTO note_version (note_id, version_no, content, save_time) VALUES (#{noteId}, #{versionNo}, #{content}, #{saveTime})")
    int insertVersion(NoteVersion noteVersion);

    @Delete("DELETE FROM note_version WHERE note_id = #{noteId} AND version_no = #{versionNo}")
    int deleteVersion(@Param("noteId") Long noteId, @Param("versionNo") Integer versionNo);

    // 获取当前最大版本号
    @Select("SELECT COALESCE(MAX(version_no), 0) FROM note_version WHERE note_id = #{noteId}")
    Integer getMaxVersionNo(Long noteId);
}