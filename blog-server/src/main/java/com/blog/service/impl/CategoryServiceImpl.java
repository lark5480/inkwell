package com.blog.service.impl;

import com.blog.common.CacheNames;
import com.blog.dto.CategoryDTO;
import com.blog.entity.Category;
import com.blog.exception.BusinessException;
import com.blog.repository.CategoryRepository;
import com.blog.service.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * C端：获取所有分类（按排序字段升序）
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.CATEGORY_LIST)
    public List<CategoryDTO> getAllCategories() {
        log.debug("C端获取所有分类");
        List<Category> categories = categoryRepository.findByIsDeletedOrderBySort(false);
        return categories.stream()
                .map(this::toCategoryDTO)
                .toList();
    }

    /**
     * 后台：创建分类
     */
    @Override
    @Transactional
    @CacheEvict(value = CacheNames.CATEGORY_LIST, allEntries = true)
    public Long createCategory(CategoryDTO dto) {
        log.info("创建分类 name={}", dto.name());
        Category category = Category.builder()
                .name(dto.name())
                .slug(dto.slug())
                .description(dto.description())
                .sort(dto.sort() != null ? dto.sort() : 0)
                .build();
        categoryRepository.save(category);
        log.info("分类创建成功 id={}", category.getId());
        return category.getId();
    }

    /**
     * 后台：更新分类
     */
    @Override
    @Transactional
    @CacheEvict(value = CacheNames.CATEGORY_LIST, allEntries = true)
    public void updateCategory(Long id, CategoryDTO dto) {
        log.info("更新分类 id={}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Category not found"));

        if (dto.name() != null) category.setName(dto.name());
        if (dto.slug() != null) category.setSlug(dto.slug());
        if (dto.description() != null) category.setDescription(dto.description());
        if (dto.sort() != null) category.setSort(dto.sort());

        categoryRepository.save(category);
        log.info("分类更新完成 id={}", id);
    }

    /**
     * 后台：删除分类
     */
    @Override
    @Transactional
    @CacheEvict(value = CacheNames.CATEGORY_LIST, allEntries = true)
    public void deleteCategory(Long id) {
        log.warn("删除分类 id={}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Category not found"));
        categoryRepository.delete(category);
    }

    /**
     * C端：获取所有分类（别名，与 getAllCategories 功能一致）
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.CATEGORY_LIST)
    public List<CategoryDTO> getCategories() {
        log.debug("C端获取分类列表");
        List<Category> categories = categoryRepository.findByIsDeletedOrderBySort(false);
        return categories.stream()
                .map(this::toCategoryDTO)
                .toList();
    }

    private CategoryDTO toCategoryDTO(Category category) {
        long articleCount = category.getArticles() != null
                ? category.getArticles().stream().filter(a -> "PUBLISHED".equals(a.getStatus())).count()
                : 0;
        return new CategoryDTO(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getSort(),
                articleCount
        );
    }
}
