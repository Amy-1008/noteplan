package com.noteplan.service.impl;

import com.noteplan.entity.NoteTag;
import com.noteplan.entity.Tag;
import com.noteplan.dto.TagDTO;
import com.noteplan.mapper.NoteTagMapper;
import com.noteplan.mapper.TagMapper;
import com.noteplan.service.TagService;
import com.noteplan.vo.TargetInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final NoteTagMapper noteTagMapper;

    //标签
    //创建标签
    @Override
    @Transactional
    public Tag createTag(TagDTO tagDTO) {
        Tag existTag = tagMapper.selectByName(tagDTO.getName());
        if (existTag != null) {
            throw new RuntimeException("标签已存在: " + tagDTO.getName());
        }
        Tag tag = new Tag();
        tag.setName(tagDTO.getName());
        tag.setRank(tagDTO.getRank() != null ? tagDTO.getRank() : 0);
        tagMapper.insert(tag);
        log.info("创建标签成功: id={}, name={}, rank={}", tag.getId(), tag.getName(), tag.getRank());
        return tag;
    }
    //删除标签
    @Override
    @Transactional
    public void deleteTag(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new RuntimeException("标签不存在: id=" + id);
        }
        tagMapper.deleteById(id);
        log.info("删除标签成功: id={}, name={}", id, tag.getName());
    }
    //修改标签
    @Override
    @Transactional
    public Tag updateTag(TagDTO tagDTO) {
        if (tagDTO.getId() == null) {
            throw new RuntimeException("标签ID不能为空");
        }

        Tag existTag = tagMapper.selectById(tagDTO.getId());
        if (existTag == null) {
            throw new RuntimeException("标签不存在: id=" + tagDTO.getId());
        }

        Tag sameNameTag = tagMapper.selectByName(tagDTO.getName());
        if (sameNameTag != null && !sameNameTag.getId().equals(tagDTO.getId())) {
            throw new RuntimeException("标签名已存在: " + tagDTO.getName());
        }

        existTag.setName(tagDTO.getName());
        existTag.setRank(tagDTO.getRank() != null ? tagDTO.getRank() : 0);
        tagMapper.update(existTag);
        log.info("更新标签成功: id={}, name={}, rank={}", existTag.getId(), existTag.getName(), existTag.getRank());
        return existTag;
    }
    //获取标签列表
    @Override
    public List<Tag> getAllTags() {
        return tagMapper.selectAll();
    }
    //根据id查询标签
    @Override
    public Tag getTagById(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new RuntimeException("标签不存在: id=" + id);
        }
        return tag;
    }

    //标签绑定
    @Override
    @Transactional
    public void bindTag(Long targetId, String targetType, Long tagId) {
        // 先删除旧的标签绑定
        noteTagMapper.deleteByTarget(targetId, targetType);
        // 如果tagId为null，说明用户仅删除当前标签，返回被删除的标签
        if (tagId == null) {
            log.info("清除目标标签: targetId={}, targetType={}", targetId, targetType);
            return;
        }

        // 检查新标签是否存在
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw new RuntimeException("标签不存在: id=" + tagId);
        }

        // 插入新的绑定
        NoteTag noteTag = new NoteTag();
        noteTag.setTargetId(targetId);
        noteTag.setTargetType(targetType);
        noteTag.setTagId(tagId);
        noteTagMapper.insert(noteTag);
        log.info("绑定标签成功: targetId={}, targetType={}, tagId={}, tagName={}",
                targetId, targetType, tagId, tag.getName());
    }

    //获取目标绑定的标签
    @Override
    public Tag getTagByTarget(Long targetId, String targetType) {
        Long tagId = noteTagMapper.selectTagIdByTarget(targetId, targetType);
        if (tagId == null) {
            return null;
        }
        return tagMapper.selectById(tagId);
    }

    //清除目标的标签
    @Override
    @Transactional
    public void clearTag(Long targetId, String targetType) {
        noteTagMapper.deleteByTarget(targetId, targetType);
        log.info("清除目标标签: targetId={}, targetType={}", targetId, targetType);
    }

    @Override
    public List<Long> getTargetIdsByTag(Long tagId, String targetType) {
        return noteTagMapper.selectTargetIdsByTag(tagId, targetType);
    }
    @Override
    public List<Long> getAllTargetIdsByType(String targetType) {
        return noteTagMapper.selectAllTargetIdsByType(targetType);
    }

    @Override
    public List<TargetInfo> getTargets(Long tagId, String targetType) {
        return noteTagMapper.selectTargets(tagId, targetType);
    }
}