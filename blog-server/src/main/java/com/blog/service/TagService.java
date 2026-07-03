package com.blog.service;

import com.blog.dto.TagDTO;

import java.util.List;

/**
 * 标签业务接口，提供标签的增删改查功能。
 */
public interface TagService {

    // Admin methods
    /**
     * 获取所有标签列表。
     *
     * @return 所有标签的列表
     */
    List<TagDTO> getAllTags();

    /**
     * 创建新标签。
     *
     * @param dto 标签数据（名称等）
     * @return 新创建标签的 ID
     */
    Long createTag(TagDTO dto);

    /**
     * 更新指定标签。
     *
     * @param id  标签 ID
     * @param dto 新的标签数据
     */
    void updateTag(Long id, TagDTO dto);

    /**
     * 删除指定标签。
     *
     * @param id 要删除的标签 ID
     */
    void deleteTag(Long id);

    // Web methods
    /**
     * 获取前端展示的标签列表（通常过滤了未使用的标签或按使用量排序）。
     *
     * @return 前端展示的标签列表
     */
    List<TagDTO> getTags();
}
