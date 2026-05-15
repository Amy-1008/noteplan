package com.noteplan.controller;

import com.noteplan.entity.Tag;
import com.noteplan.dto.TagDTO;
import com.noteplan.service.TagService;
import com.noteplan.vo.Result;
import com.noteplan.vo.TargetInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Validated
public class TagController {

    private final TagService tagService;

    //标签
    //新增标签
    @PostMapping
    public Result<Tag> createTag(@Valid @RequestBody TagDTO tagDTO) {
        Tag tag = tagService.createTag(tagDTO);
        return Result.success(tag);
    }
    //删除标签
    @DeleteMapping("/{id}")
    public Result<Void> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return Result.success();
    }
    //修改标签
    @PutMapping("/{id}")
    public Result<Tag> updateTag(@PathVariable Long id, @Valid @RequestBody TagDTO tagDTO) {
        tagDTO.setId(id);
        Tag tag = tagService.updateTag(tagDTO);
        return Result.success(tag);
    }
    //获取全部标签
    @GetMapping
    public Result<List<Tag>> getAllTags() {
        List<Tag> tags = tagService.getAllTags();
        return Result.success(tags);
    }
    //根据id查询标签
    @GetMapping("/{id}")
    public Result<Tag> getTagById(@PathVariable Long id) {
        Tag tag = tagService.getTagById(id);
        return Result.success(tag);
    }
    //标签绑定

   //为目标绑定/修改标签（若传入null则代表解绑当前标签）
    @PostMapping("/bind")
    public Result<Void> bindTag(@RequestParam Long targetId,
                                @RequestParam String targetType,
                                @RequestParam(required = false) Long tagId) {
        tagService.bindTag(targetId, targetType, tagId);
        return Result.success();
    }

    //根据目标id获取目前的标签
    @GetMapping("/target")
    public Result<Tag> getTagByTarget(@RequestParam Long targetId,
                                      @RequestParam String targetType) {
        Tag tag = tagService.getTagByTarget(targetId, targetType);
        return Result.success(tag);
    }

    //清除目标标签
    @DeleteMapping("/clear")
    public Result<Void> clearTag(@RequestParam Long targetId,
                                 @RequestParam String targetType) {
        tagService.clearTag(targetId, targetType);
        return Result.success();
    }

    //根据传入的标签id及目标类型进行筛选，并返回筛选对象的id及类型
    @GetMapping("/filter")
    public Result<List<TargetInfo>> filterByTag(@RequestParam(required = false) Long tagId,
                                                @RequestParam(required = false) String targetType) {
        List<TargetInfo> targets = tagService.getTargets(tagId, targetType);
        return Result.success(targets);
    }
}