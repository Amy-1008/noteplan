package com.noteplan.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ScheduleNoteMapper {

    // 插入日程关联笔记
    @Insert("INSERT INTO schedule_note (schedule_id, note_id) VALUES (#{scheduleId}, #{noteId})")
    int insert(@Param("scheduleId") Long scheduleId, @Param("noteId") Long noteId);
}