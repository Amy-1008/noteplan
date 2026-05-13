package com.noteplan.mapper;

import com.noteplan.entity.Schedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ScheduleMapper {

    // 查询所有未删除的日程，按 start_time 升序（日期最小的在前）
    @Select("SELECT * FROM schedule WHERE status = 0 ORDER BY start_time ASC")
    List<Schedule> findAll();

    @Update("UPDATE schedule SET completed = #{completed} WHERE id = #{id}")
    void updateComplete(@Param("id") Long id, @Param("completed") Integer completed);
}