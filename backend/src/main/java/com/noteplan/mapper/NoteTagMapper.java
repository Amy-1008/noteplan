package com.noteplan.mapper;

import com.noteplan.entity.NoteTag;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface NoteTagMapper {


    //绑定标签到目标（笔记/日程）已有标签则先解绑
    @Insert("INSERT INTO note_tag (target_id, target_type, tag_id) VALUES (#{targetId}, #{targetType}, #{tagId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(NoteTag noteTag);


    //删除目标的所有标签（绑定新标签前先调用）
    @Delete("DELETE FROM note_tag WHERE target_id = #{targetId} AND target_type = #{targetType}")
    int deleteByTarget(@Param("targetId") Long targetId,
                       @Param("targetType") String targetType);


    //查询目标的标签ID
    @Select("SELECT tag_id FROM note_tag WHERE target_id = #{targetId} AND target_type = #{targetType} LIMIT 1")
    Long selectTagIdByTarget(@Param("targetId") Long targetId,
                             @Param("targetType") String targetType);


    //查询目标的标签关联记录
    @Select("SELECT id, target_id, target_type, tag_id FROM note_tag WHERE target_id = #{targetId} AND target_type = #{targetType}")
    NoteTag selectByTarget(@Param("targetId") Long targetId,
                           @Param("targetType") String targetType);

    //根据标签ID查询绑定的目标ID列表
    @Select("SELECT target_id FROM note_tag WHERE tag_id = #{tagId} " +
            "AND (#{targetType} IS NULL OR target_type = #{targetType})")
    List<Long> selectTargetIdsByTag(@Param("tagId") Long tagId,
                                    @Param("targetType") String targetType);
    //筛选所有关联了标签的日程or笔记的ids
    @Select("SELECT DISTINCT target_id FROM note_tag WHERE target_type = #{targetType}")
    List<Long> selectAllTargetIdsByType(@Param("targetType") String targetType);
}