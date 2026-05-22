package com.noteplan.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface CalendarMapper {

    /**
     * 获取某月的所有事件（使用视图）
     */
    @Select("SELECT * FROM v_calendar_month_events " +
            "WHERE YEAR(event_date) = #{year} AND MONTH(event_date) = #{month} " +
            "ORDER BY event_date, event_time")
    List<Map<String, Object>> getMonthEvents(
            @Param("year") int year,
            @Param("month") int month);

    /**
     * 获取某月的统计数据（使用视图）
     */
    @Select("SELECT * FROM v_month_statistics " +
            "WHERE year = #{year} AND month = #{month}")
    Map<String, Object> getMonthStatistics(
            @Param("year") int year,
            @Param("month") int month);

    /**
     * 获取某天的日程详情（使用视图）
     */
    @Select("SELECT * FROM v_schedule_detail " +
            "WHERE DATE(end_time) = #{date}")
    List<Map<String, Object>> getScheduleDetailByDate(
            @Param("date") String date);

    /**
     * 获取某天的笔记详情（使用视图）
     */
    @Select("SELECT * FROM v_note_detail " +
            "WHERE DATE(create_time_display) = #{date} " +
            "ORDER BY create_time_display")
    List<Map<String, Object>> getNoteDetailByDate(
            @Param("date") String date);
}