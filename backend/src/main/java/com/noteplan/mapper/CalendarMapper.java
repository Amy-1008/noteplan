package com.noteplan.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface CalendarMapper {

    /**
     * 查询某月的日程
     */
    @Select("SELECT " +
            "DATE(end_time) as event_date, " +
            "id, " +
            "title, " +
            "'schedule' as type, " +
            "DATE_FORMAT(end_time, '%H:%i') as event_time, " +
            "CASE WHEN completed = 1 THEN 'completed' ELSE 'pending' END as status " +
            "FROM schedule " +
            "WHERE YEAR(end_time) = #{year} " +
            "AND MONTH(end_time) = #{month} " +
            "AND status = 0")
    List<Map<String, Object>> getSchedulesByMonth(@Param("year") int year, @Param("month") int month);

    /**
     * 查询某月的笔记
     */
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
            "id, " +
            "title, " +
            "CASE " +
            "   WHEN start_time IS NOT NULL " +
            "       THEN CONCAT(DATE_FORMAT(start_time, '%H:%i'), ' - ', DATE_FORMAT(end_time, '%H:%i')) " +
            "   ELSE DATE_FORMAT(end_time, '%H:%i') " +
            "END as time_display, " +
            "remark, " +
            "CASE WHEN completed = 1 THEN 'completed' ELSE 'pending' END as status " +
            "FROM schedule " +
            "WHERE DATE(end_time) = #{date} " +
            "AND status = 0 " +
            "ORDER BY end_time")
    List<Map<String, Object>> getScheduleDetailByDate(@Param("date") String date);

    /**
     * 查询某天的笔记详情（带标签）
     */
    @Select("SELECT " +
            "n.id, " +
            "n.title, " +
            "n.content, " +
            "DATE_FORMAT(n.create_time, '%H:%i') as time_only, " +
            "n.create_time, " +
            "GROUP_CONCAT(DISTINCT t.name) as tags " +
            "FROM note n " +
            "LEFT JOIN note_tag nt ON n.id = nt.target_id AND nt.target_type = 'NOTE' " +
            "LEFT JOIN tag t ON nt.tag_id = t.id " +
            "WHERE DATE(n.create_time) = #{date} " +
            "AND n.status = 0 " +
            "GROUP BY n.id, n.title, n.content, n.create_time " +
            "ORDER BY n.create_time")
    List<Map<String, Object>> getNoteDetailByDate(@Param("date") String date);

    /**
     * 获取月份统计数据 - 修复版
     */
    @Select("SELECT " +
            "COALESCE(SUM(CASE WHEN type = 'schedule' THEN 1 ELSE 0 END), 0) as total_schedules, " +
            "COALESCE(SUM(CASE WHEN type = 'note' THEN 1 ELSE 0 END), 0) as total_notes, " +
            "COALESCE(COUNT(DISTINCT event_date), 0) as marked_days " +
            "FROM ( " +
            "  SELECT DATE(end_time) as event_date, 'schedule' as type FROM schedule " +
            "  WHERE YEAR(end_time) = #{year} AND MONTH(end_time) = #{month} AND status = 0 " +
            "  UNION ALL " +
            "  SELECT DATE(create_time) as event_date, 'note' as type FROM note " +
            "  WHERE YEAR(create_time) = #{year} AND MONTH(create_time) = #{month} AND status = 0 " +
            ") t")
    Map<String, Object> getMonthStatistics(@Param("year") int year, @Param("month") int month);
}