package com.noteplan.service;

import com.noteplan.entity.Tag;
import com.noteplan.dto.TagDTO;
import java.util.List;

public interface TagService {

    //标签
    Tag createTag(TagDTO tagDTO);
    void deleteTag(Long id);
    Tag updateTag(TagDTO tagDTO);
    List<Tag> getAllTags();
    Tag getTagById(Long id);

    //标签绑定
    /**
     * 为目标绑定标签（自动替换旧的标签）
     * @param targetId 目标ID
     * @param targetType 目标类型（NOTE 或 SCHEDULE）
     * @param tagId 标签ID，传null表示清除标签
     */
    void bindTag(Long targetId, String targetType, Long tagId);

    /**
     * 获取目标的标签
     * @return 标签对象，如果没有绑定则返回null
     */
    Tag getTagByTarget(Long targetId, String targetType);

    /**
     * 清除目标的标签
     */
    void clearTag(Long targetId, String targetType);

    /**
     * 根据标签筛选目标ID列表
     */
    List<Long> getTargetIdsByTag(Long tagId, String targetType);
}