package com.blog.service.impl;

import com.blog.common.CacheNames;
import com.blog.dto.TagDTO;
import com.blog.entity.Tag;
import com.blog.exception.BusinessException;
import com.blog.repository.TagRepository;
import com.blog.service.TagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TagServiceImpl implements TagService {

    private static final Logger log = LoggerFactory.getLogger(TagServiceImpl.class);

    private final TagRepository tagRepository;

    public TagServiceImpl(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    /**
     * C端：获取所有标签
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.TAG_LIST)
    public List<TagDTO> getAllTags() {
        log.debug("C端获取所有标签");
        List<Tag> tags = tagRepository.findAll();
        return tags.stream()
                .map(this::toTagDTO)
                .toList();
    }

    /**
     * 后台：创建标签
     */
    @Override
    @Transactional
    @CacheEvict(value = CacheNames.TAG_LIST, allEntries = true)
    public Long createTag(TagDTO dto) {
        log.info("创建标签 name={}", dto.name());
        Tag tag = Tag.builder()
                .name(dto.name())
                .slug(dto.slug())
                .build();
        tagRepository.save(tag);
        log.info("标签创建成功 id={}", tag.getId());
        return tag.getId();
    }

    /**
     * 后台：更新标签
     */
    @Override
    @Transactional
    @CacheEvict(value = CacheNames.TAG_LIST, allEntries = true)
    public void updateTag(Long id, TagDTO dto) {
        log.info("更新标签 id={}", id);
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Tag not found"));

        if (dto.name() != null) tag.setName(dto.name());
        if (dto.slug() != null) tag.setSlug(dto.slug());

        tagRepository.save(tag);
        log.info("标签更新完成 id={}", id);
    }

    /**
     * 后台：删除标签
     */
    @Override
    @Transactional
    @CacheEvict(value = CacheNames.TAG_LIST, allEntries = true)
    public void deleteTag(Long id) {
        log.warn("删除标签 id={}", id);
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Tag not found"));
        tagRepository.delete(tag);
    }

    /**
     * C端：获取所有标签（别名，与 getAllTags 功能一致）
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.TAG_LIST)
    public List<TagDTO> getTags() {
        log.debug("C端获取标签列表");
        List<Tag> tags = tagRepository.findAll();
        return tags.stream()
                .map(this::toTagDTO)
                .toList();
    }

    private TagDTO toTagDTO(Tag tag) {
        long articleCount = tag.getArticles() != null ? tag.getArticles().size() : 0;
        return new TagDTO(
                tag.getId(),
                tag.getName(),
                tag.getSlug(),
                articleCount
        );
    }
}
