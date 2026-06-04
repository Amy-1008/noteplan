package com.noteplan.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ScheduleNoteMapper {

    // 插入日程关联笔记
    @Insert("INSERT INTO schedule_note (schedule_id, note_id) VALUES (#{scheduleId}, #{noteId})")
    int insert(@Param("scheduleId") Long scheduleId, @Param("noteId") Long noteId);

    // 删除日程的所有笔记关联
    @Delete("DELETE FROM schedule_note WHERE schedule_id = #{scheduleId}")
    int deleteByScheduleId(@Param("scheduleId") Long scheduleId);

    @Select("SELECT note_id FROM schedule_note WHERE schedule_id = #{scheduleId}")
    List<Long> findNoteIdsByScheduleId(@Param("scheduleId") Long scheduleId);
}