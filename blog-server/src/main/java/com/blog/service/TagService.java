package com.blog.service;

import com.blog.dto.TagDTO;

import java.util.List;

public interface TagService {

    // Admin methods
    List<TagDTO> getAllTags();

    Long createTag(TagDTO dto);

    void updateTag(Long id, TagDTO dto);

    void deleteTag(Long id);

    // Web methods
    List<TagDTO> getTags();
}
