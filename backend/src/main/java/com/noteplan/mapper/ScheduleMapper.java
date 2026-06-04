package com.noteplan.mapper;

import com.noteplan.entity.Schedule;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;

import java.util.List;

@Mapper
public interface ScheduleMapper {

    // 查询所有未删除的日程
    @Select("SELECT * FROM schedule WHERE status = 0 ORDER BY end_time ASC")
    List<Schedule> findAll();
    // 根据ID列表查询日程（新增）
    @Select("<script>" +
            "SELECT * FROM schedule WHERE status = 0 AND id IN " +
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " ORDER BY end_time ASC" +
            "</script>")
    List<Schedule> findByIds(@Param("ids") List<Long> ids);

    // 根据ID查询日程
    @Select("SELECT * FROM schedule WHERE id = #{id}")
    Schedule findById(Long id);

    // 新增日程
    @Insert("INSERT INTO schedule (title, start_time, end_time, repeat_rule, remark, completed, status, `rank`) " +
            "VALUES (#{title}, #{startTime}, #{endTime}, #{repeatRule}, #{remark}, #{completed}, #{status}, #{rank})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Schedule schedule);

    // 更新完成状态
    @Update("UPDATE schedule SET completed = #{completed} WHERE id = #{id}")
    int updateComplete(@Param("id") Long id, @Param("completed") Integer completed);

    // 更新时间（重复日程用）
    @Update("UPDATE schedule SET start_time = #{startTime}, end_time = #{endTime} WHERE id = #{id}")
    int updateTime(@Param("id") Long id, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    // 更新日程
    @Update("UPDATE schedule SET title = #{title}, start_time = #{startTime}, end_time = #{endTime}, " +
            "repeat_rule = #{repeatRule}, remark = #{remark} WHERE id = #{id}")
    int update(Schedule schedule);

    // 删除
    @Update("UPDATE schedule SET status = 1 WHERE id = #{id}")
    int deleteById(Long id);
}