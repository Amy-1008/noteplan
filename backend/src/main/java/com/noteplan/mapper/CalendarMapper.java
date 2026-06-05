package com.noteplan.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface CalendarMapper {

    @Select("SELECT " +
            "DATE(end_time) as event_date, " +
            "id, " +
            "title, " +
            "'schedule' as type, " +
            "DATE_FORMAT(end_time, '%H:%i') as event_time, " +
            "start_time, " +
            "end_time, " +
            "CASE WHEN completed = 1 THEN 'completed' ELSE 'pending' END as status " +
            "FROM schedule " +
            "WHERE YEAR(end_time) = #{year} " +
            "AND MONTH(end_time) = #{month} " +
            "AND status = 0 " +
            "UNION " +
            "SELECT " +
            "DATE(start_time) as event_date, " +
            "id, " +
            "title, " +
            "'schedule' as type, " +
            "DATE_FORMAT(start_time, '%H:%i') as event_time, " +
            "start_time, " +
            "end_time, " +
            "CASE WHEN completed = 1 THEN 'completed' ELSE 'pending' END as status " +
            "FROM schedule " +
            "WHERE start_time IS NOT NULL " +
            "AND YEAR(start_time) = #{year} " +
            "AND MONTH(start_time) = #{month} " +
            "AND status = 0")
    List<Map<String, Object>> getSchedulesByMonth(@Param("year") int year, @Param("month") int month);

    @Select("SELECT " +
            "DATE(create_time) as event_date, " +
            "id, " +
            "title, " +
            "'note' as type, " +
            "DATE_FORMAT(create_time, '%H:%i') as event_time " +
            "FROM note " +
            "WHERE YEAR(create_time) = #{year} " +
            "AND MONTH(create_time) = #{month} " +
            "AND status = 0")
    List<Map<String, Object>> getNotesByMonth(@Param("year") int year, @Param("month") int month);
    /**
     * 查询某天的日程详情
     */
    @Select("SELECT " +
            "s.id, " +
            "s.title, " +
            "CASE " +
            "   WHEN s.start_time IS NOT NULL " +
            "       THEN CONCAT(DATE_FORMAT(s.start_time, '%H:%i'), ' - ', DATE_FORMAT(s.end_time, '%H:%i')) " +
            "   ELSE DATE_FORMAT(s.end_time, '%H:%i') " +
            "END as time_display, " +
            "s.remark, " +
            "CASE WHEN s.completed = 1 THEN 'completed' ELSE 'pending' END as status, " +
            "GROUP_CONCAT(DISTINCT t.name) as tags " +
            "FROM schedule s " +
            "LEFT JOIN note_tag nt ON s.id = nt.target_id AND nt.target_type = 'SCHEDULE' " +
            "LEFT JOIN tag t ON nt.tag_id = t.id " +
            "WHERE s.status = 0 " +
            "AND ( " +
            "  (s.start_time IS NULL AND DATE(s.end_time) = #{date}) " +
            "  OR (s.start_time IS NOT NULL AND #{date} BETWEEN DATE(s.start_time) AND DATE(s.end_time)) " +
            ") " +
            "GROUP BY s.id, s.title, s.start_time, s.end_time, s.remark, s.completed " +
            "ORDER BY s.end_time")
    List<Map<String, Object>> getScheduleDetailByDate(@Param("date") String date);

    @Select("SELECT " +
            "n.id, " +
            "n.title, " +
            "n.content, " +
            "DATE_FORMAT(n.create_time, '%H:%i') as time_only, " +
            "GROUP_CONCAT(DISTINCT t.name) as tags " +
            "FROM note n " +
            "LEFT JOIN note_tag nt ON n.id = nt.target_id AND nt.target_type = 'NOTE' " +
            "LEFT JOIN tag t ON nt.tag_id = t.id " +
            "WHERE DATE(n.create_time) = #{date} " +
            "AND n.status = 0 " +
            "GROUP BY n.id, n.title, n.content, n.create_time")
    List<Map<String, Object>> getNoteDetailByDate(@Param("date") String date);
}