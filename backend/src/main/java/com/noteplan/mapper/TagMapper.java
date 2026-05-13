package com.noteplan.mapper;

import com.noteplan.entity.Tag;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TagMapper {
    //新增标签
    @Insert("INSERT INTO tag (name) VALUES (#{name})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Tag tag);

    //删除标签
    @Delete("DELETE FROM tag WHERE id = #{id}")
    int deleteById(Long id);

    //更改标签
    @Update("UPDATE tag SET name = #{name} WHERE id = #{id}")
    int update(Tag tag);


    //根据ID查询标签
    @Select("SELECT id, name, create_time FROM tag WHERE id = #{id}")
    Tag selectById(Long id);


    //查询所有标签
    @Select("SELECT id, name, create_time FROM tag ORDER BY id")
    List<Tag> selectAll();


    //根据名称查询标签（用于去重）
    @Select("SELECT id, name, create_time FROM tag WHERE name = #{name}")
    Tag selectByName(String name);
}